package ru.ism.mybankdto.exception;

public class NoFoundException extends RuntimeException {
    public NoFoundException(String message) {
        super(message);
    }
}
