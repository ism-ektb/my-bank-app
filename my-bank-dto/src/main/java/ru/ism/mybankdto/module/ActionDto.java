package ru.ism.mybankdto.module;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ActionDto(@Positive(message = "Сумма должна быть положительной") long sum, @NotNull Action action) {
}
