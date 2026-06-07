package ru.ism.mybankdto.module;

import java.time.LocalDate;

public record AccountRequestDto(String name, LocalDate birthdate) {
}
