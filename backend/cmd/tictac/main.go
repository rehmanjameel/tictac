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

	procedureCreateAccount = "io.xconn.tictac.account.create"

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

	defer session.Unregister(reg.ID)

	onSub, err := session.Subscribe(topicSetOnline, userManager.statusOnlineHandler(db), nil)
	if err != nil {
		log.Fatal(err)
	}
	defer session.Unsubscribe(onSub)

	offSub, err := session.Subscribe(topicSetOffline, userManager.statusOfflineHandler, nil)
	if err != nil {
		log.Fatal(err)
	}
	defer session.Unsubscribe(offSub)

	// Close if SIGINT (CTRL-c) received.
	closeChan := make(chan os.Signal, 1)
	signal.Notify(closeChan, os.Interrupt)
	select {
	case <-closeChan:
	case <-session.LeaveChan():
	}
}
