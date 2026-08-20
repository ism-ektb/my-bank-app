package ru.ism.mybankdto.module;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import ru.ism.mybankdto.annotation.SenderAndReceiverMustDiffer;

@SenderAndReceiverMustDiffer(sender = "sender", receiver = "receiver")
public record Transfer(@NotBlank String sender,
                       @NotBlank String receiver,
                       @Positive(message = "Sum must be positive") long sum) {
}
