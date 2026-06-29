package ru.ism.mybankaccountapp.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@Table(name = "accounts", schema = "account_service")
public class Account {

    @Id
    private long id;
    private String login;
    @Column(value = "username")
    private String name;
    private LocalDate birthdate = LocalDate.of(1900, 1, 1);
    private long balance = 0;

}

