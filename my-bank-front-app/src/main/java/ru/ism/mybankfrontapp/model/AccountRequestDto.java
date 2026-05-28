package ru.ism.mybankfrontapp.model;

import java.time.LocalDate;

public record AccountRequestDto(String name, LocalDate birthdate) {
}
