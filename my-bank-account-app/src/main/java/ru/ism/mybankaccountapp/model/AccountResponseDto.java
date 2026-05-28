package ru.ism.mybankaccountapp.model;

import java.time.LocalDate;

public record AccountResponseDto(String name, LocalDate birthdate, String login, long balance) {
}
