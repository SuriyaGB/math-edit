package com.gbrit.exception;

public class CollectionNotFoundException extends RuntimeException{
    public CollectionNotFoundException(String message){
        super(message);
    }
}
