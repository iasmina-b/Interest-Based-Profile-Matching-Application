package org.example;

public class DuplicateProfileException extends Exception {

    public DuplicateProfileException(String username) {
        super("Error: A profile with the username '" + username + "' already exists. Please try again.");
    }
}