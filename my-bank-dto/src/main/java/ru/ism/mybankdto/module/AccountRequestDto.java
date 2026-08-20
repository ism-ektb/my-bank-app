package ru.ism.mybankdto.module;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record AccountRequestDto(@NotBlank String name, LocalDate birthdate) {
}
