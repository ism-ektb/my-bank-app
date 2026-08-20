package ru.ism.mybankdto.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import ru.ism.mybankdto.module.Transfer;

public class SenderAndReceiverMustDifferValidator implements ConstraintValidator<SenderAndReceiverMustDiffer, Transfer> {
    @Override
    public boolean isValid(Transfer value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        if (value.sender() == null || value.receiver() == null) {
            return false;
        }
        String sender = value.sender();
        String receiver = value.receiver();
        return !sender.equals(receiver);
    }

    @Override
    public void initialize(SenderAndReceiverMustDiffer constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }
}
