/**
 * DTO для обновления данных клиента и его банковского счёта.
 *
 * <p>Содержит основную информацию пользователя, необходимую для создания
 * или обновления профиля в рамках аккаунт-сервиса.</p>
 *
 * @param name имя клиента
 * @param birthdate дата рождения клиента
 */
package ru.ism.mybankdto.module;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record AccountRequestDto(@NotBlank String name, LocalDate birthdate) {
}
