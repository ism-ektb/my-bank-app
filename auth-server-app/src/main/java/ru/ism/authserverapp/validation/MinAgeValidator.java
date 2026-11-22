package ru.ism.authserverapp.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;

public class MinAgeValidator implements ConstraintValidator<MinAge, LocalDate> {

    private int minAge;

    @Override
    public void initialize(MinAge constraintAnnotation) {
        this.minAge = constraintAnnotation.value();
    }

    @Override
    public boolean isValid(LocalDate birthDate, ConstraintValidatorContext context) {
        if (birthDate == null) {
            return true; // null обрабатывает @NotNull отдельно
        }

        LocalDate today = LocalDate.now();
        LocalDate minAllowedDate = today.minusYears(minAge);

        return !birthDate.isAfter(minAllowedDate);
    }
}