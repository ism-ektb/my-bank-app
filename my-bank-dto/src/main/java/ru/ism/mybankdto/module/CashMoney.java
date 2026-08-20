package ru.ism.mybankdto.module;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CashMoney(@NotBlank String login, @Positive(message = "Сумма должна быть положительной") long sum) {
}
