package ru.ism.frontapp.dto;

public record LoginRequest(
        String login,
        String password
) {
}
