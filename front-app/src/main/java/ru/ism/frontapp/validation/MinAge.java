package ru.ism.frontapp.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = MinAgeValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface MinAge {

    String message() default "Пользователю должно быть не меньше {value} лет";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int value();
}