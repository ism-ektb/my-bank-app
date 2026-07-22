package ru.ism.mybankdto.module;

import jakarta.validation.constraints.Positive;

public record ActionDto(@Positive(message = "Сумма должна быть положительной") long sum, Action action) {
}
