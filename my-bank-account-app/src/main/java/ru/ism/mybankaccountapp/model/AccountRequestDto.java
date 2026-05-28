package ru.ism.mybankaccountapp.model;

import java.time.LocalDate;

public record AccountRequestDto(String name, LocalDate birthdate) {
}
