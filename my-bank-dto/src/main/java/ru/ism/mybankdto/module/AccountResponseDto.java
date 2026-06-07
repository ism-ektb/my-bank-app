package ru.ism.mybankdto.module;

import java.time.LocalDate;

public record AccountResponseDto(String name, LocalDate birthdate, String login, long balance) {
}
