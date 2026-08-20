package ru.ism.mybankdto.exception;

public class ClientException extends RuntimeException {
    public ClientException(String message) {
        super(message);
    }
}
