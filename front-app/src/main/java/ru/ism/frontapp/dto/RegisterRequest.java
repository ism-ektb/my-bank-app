package ru.ism.frontapp.dto;

import java.time.LocalDate;

public record RegisterRequest(
        String name,
        String login,
        LocalDate birthDate,
        String password
) {
}
