package ru.ism.authserverapp.web;



import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.ism.authserverapp.validation.MinAge;

import java.time.LocalDate;

public class RegisterForm {

    @NotBlank(message = "Имя обязательно")
    private String name;

    @NotBlank(message = "Логин обязателен")
    private String login;

    @NotBlank(message = "Пароль обязателен")
    @Size(min = 6, message = "Пароль должен содержать минимум 6 символов")
    private String password;

    @NotNull(message = "Дата рождения обязательна")
    @MinAge(value = 18, message = "Пользователю должно быть не меньше 18 лет")
    private LocalDate birthDate;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }
}
