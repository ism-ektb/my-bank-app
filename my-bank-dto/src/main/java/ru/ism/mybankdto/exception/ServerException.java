package ru.ism.mybankdto.exception;

public class ServerException extends RuntimeException {
    public ServerException(String message) {
        super(message);
    }
}
