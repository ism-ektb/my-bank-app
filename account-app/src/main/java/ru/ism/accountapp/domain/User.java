package ru.ism.accountapp.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Table("app_users")
public record User(
        @Id Long id,
        @Column("username")
        String name,
        String login,
        @Column("birth_date")
        LocalDate birthDate,
        String password,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}