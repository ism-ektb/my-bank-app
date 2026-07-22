package ru.ism.mybankdto.module;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TransferRequest(@NotBlank @NotNull String name, @Positive long sum) {
}
