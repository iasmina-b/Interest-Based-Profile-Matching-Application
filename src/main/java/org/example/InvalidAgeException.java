package org.example;

public class InvalidAgeException extends Exception {

    public InvalidAgeException(int age) {
        super("Error: Invalid age '" + age + "'. Age must be between 0 and 150.");
    }
}