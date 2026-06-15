package com.gbrit.exception;

public class DuplicateUserNameException extends Exception {
    public DuplicateUserNameException(String message){
        super(message);
    }
}

