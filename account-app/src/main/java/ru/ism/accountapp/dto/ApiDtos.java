package ru.ism.accountapp.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ru.ism.accountapp.domain.Account;
import ru.ism.accountapp.domain.Currency;
import ru.ism.accountapp.domain.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ApiDtos {

    public record UserCreateRequest(
            @NotBlank String name,
            @NotBlank String login,
            LocalDate birthDate,
            @NotBlank String password
    ) {
    }

    public record UserUpdateRequest(
            String name,
            LocalDate birthDate,
            String password
    ) {
    }

    public record UserResponse(
            Long id,
            String name,
            String login,
            LocalDate birthDate,
            String password,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static UserResponse from(User user) {
            return new UserResponse(
                    user.id(),
                    user.name(),
                    user.login(),
                    user.birthDate(),
                    user.password(),
                    user.createdAt(),
                    user.updatedAt()
            );
        }
    }

    public record AccountCreateRequest(
            @NotNull Currency currency,
            @NotNull @DecimalMin(value = "0.00", inclusive = true) BigDecimal initialBalance
    ) {
    }

    public record AccountResponse(
            Long id,
            String userLogin,
            String number,
            BigDecimal balance,
            String currency,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static AccountResponse from(Account account) {
            return new AccountResponse(
                    account.id(),
                    account.userLogin(),
                    account.number(),
                    account.balance(),
                    account.currency(),
                    account.createdAt(),
                    account.updatedAt()
            );
        }
    }

    public record BalanceChangeRequest(
            @NotNull BigDecimal delta
    ) {
    }

    public record UserAuthResponse(
            Long id,
            String name,
            String login,
            LocalDate birthDate,
            String password) {
        public static UserAuthResponse from(User user) {
            return new UserAuthResponse(
                    user.id(),
                    user.name(),
                    user.login(),
                    user.birthDate(),
                    user.password()
            );
        }
    }
}
