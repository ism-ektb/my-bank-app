package ru.ism.mybankdto.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SenderAndReceiverMustDifferValidator.class)
public @interface SenderAndReceiverMustDiffer {

    String sender();
    String receiver();
    String message() default "Поле '${sender}' не должно быть равно полю '${receiver}'";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
