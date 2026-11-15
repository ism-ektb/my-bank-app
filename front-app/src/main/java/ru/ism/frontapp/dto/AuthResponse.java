package ru.ism.frontapp.dto;

import java.time.LocalDate;

public record AuthResponse(
        Long id,
        String name,
        String login,
        LocalDate birthDate,
        String phone
) {
}
