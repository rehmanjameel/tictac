package main

import (
	"fmt"
	"time"

	"gorm.io/driver/sqlite"
	"gorm.io/gorm"
)

// User struct represents the user table.
type User struct {
	ID        uint      `gorm:"primaryKey;autoIncrement" json:"id"`
	Name      string    `gorm:"not null" json:"name"`
	Email     string    `gorm:"unique;not null" json:"email"`
	CreatedAt time.Time `gorm:"autoCreateTime" json:"created_at"`
}

func initDB() (*gorm.DB, error) {
	db, err := gorm.Open(sqlite.Open("users.db"), &gorm.Config{})
	if err != nil {
		return nil, fmt.Errorf("failed to connect to database: %w", err)
	}

	if err = db.AutoMigrate(&User{}); err != nil {
		return nil, fmt.Errorf("failed to auto migrate users: %w", err)
	}

	return db, nil
}

// CreateUser inserts a new user into the database.
func CreateUser(db *gorm.DB, name, email string) (User, error) {
	user := User{Name: name, Email: email}
	if err := db.Create(&user).Error; err != nil {
		return User{}, err
	}
	return user, nil
}

// GetUsers retrieves all users from the database.
func GetUsers(db *gorm.DB) ([]User, error) {
	var users []User
	err := db.Find(&users).Error
	return users, err
}

// GetUserByID retrieves a user from the database by ID.
func GetUserByID(db *gorm.DB, id uint) (User, error) {
	var user User
	err := db.First(&user, id).Error
	return user, err
}
