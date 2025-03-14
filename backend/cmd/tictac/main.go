package main

import (
	"context"
	"log"
	"os"
	"os/signal"

	"github.com/xconnio/wampproto-go"
	"github.com/xconnio/wampproto-go/util"
	"github.com/xconnio/xconn-go"
)

const (
	url   = "ws://localhost:8080/ws"
	realm = "realm1"

	procedureCreateAccount  = "io.xconn.tictac.account.create"
	procedureGetUsers       = "io.xconn.tictac.users.get"
	procedureGetOnlineUsers = "io.xconn.tictac.users.online"

	topicSetOnline  = "io.xconn.tictac.user.online.set"
	topicSetOffline = "io.xconn.tictac.user.offline.set"

	errOperationFailed = "io.xconn.tictac.operation_failed"
)

func main() {
	db, err := initDB()
	if err != nil {
		log.Fatal(err)
	}

	userManager := NewUserManager()

	session, err := xconn.Connect(context.Background(), url, realm)
	if err != nil {
		log.Fatal(err)
	}
	defer session.Leave()

	reg, err := session.Register(procedureCreateAccount,
		func(ctx context.Context, invocation *xconn.Invocation) *xconn.Result {
			if len(invocation.Arguments) != 2 {
				return &xconn.Result{Err: wampproto.ErrInvalidArgument, Arguments: []any{"Must be called with exactly two arguments"}}
			}

			name, ok := util.AsString(invocation.Arguments[0])
			if !ok {
				return &xconn.Result{Err: wampproto.ErrInvalidArgument, Arguments: []any{"First argument must be a string"}}
			}

			email, ok := util.AsString(invocation.Arguments[1])
			if !ok {
				return &xconn.Result{Err: wampproto.ErrInvalidArgument, Arguments: []any{"Second argument must be a string"}}
			}

			user, err := CreateUser(db, name, email)
			if err != nil {
				return &xconn.Result{Err: errOperationFailed, Arguments: []any{err.Error()}}
			}

			return &xconn.Result{Arguments: []any{user}}
		}, nil)
	if err != nil {
		log.Fatal(err)
	}
	log.Printf("Registered procedure %s", procedureCreateAccount)
	defer session.Unregister(reg.ID)

	getReg, err := session.Register(procedureGetUsers, func(ctx context.Context, invocation *xconn.Invocation) *xconn.Result {
		users, err := GetUsers(db)
		if err != nil {
			return &xconn.Result{Err: errOperationFailed, Arguments: []any{err.Error()}}
		}

		return &xconn.Result{Arguments: []any{users}}
	}, nil)
	if err != nil {
		log.Fatal(err)
	}
	log.Printf("Registered procedure %s", procedureGetUsers)
	defer session.Unregister(getReg.ID)

	onSub, err := session.Subscribe(topicSetOnline, userManager.statusOnlineHandler(db), nil)
	if err != nil {
		log.Fatal(err)
	}
	log.Printf("Subscribed to %s", topicSetOnline)
	defer session.Unsubscribe(onSub)

	offSub, err := session.Subscribe(topicSetOffline, userManager.statusOfflineHandler, nil)
	if err != nil {
		log.Fatal(err)
	}
	log.Printf("Subscribed to %s", topicSetOffline)
	defer session.Unsubscribe(offSub)

	onlineUsersReg, err := session.Register(procedureGetOnlineUsers, userManager.onlineUser, nil)
	if err != nil {
		log.Fatal(err)
	}
	log.Printf("Registered procedure %s", procedureGetOnlineUsers)
	defer session.Unregister(onlineUsersReg.ID)

	// Close if SIGINT (CTRL-c) received.
	closeChan := make(chan os.Signal, 1)
	signal.Notify(closeChan, os.Interrupt)
	select {
	case <-closeChan:
	case <-session.LeaveChan():
	}
}
