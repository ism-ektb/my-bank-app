package ru.ism.mybankdto.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import ru.ism.mybankdto.module.Transfer;

public class SenderAndReceiverMustDifferValidator implements ConstraintValidator<SenderAndReceiverMustDiffer, Object> {
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        String sender = ((Transfer) value).sender();
        String receiver = ((Transfer) value).receiver();
        return !sender.equals(receiver);     }

    @Override
    public void initialize(SenderAndReceiverMustDiffer constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }
}
