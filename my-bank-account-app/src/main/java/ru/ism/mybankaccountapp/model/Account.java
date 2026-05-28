package ru.ism.mybankaccountapp.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@Entity
@Table(name = "accounts", schema = "account_service")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "accounts_seq")
    @SequenceGenerator(name = "accounts_seq", sequenceName = "accounts_sequence", allocationSize = 1)
    private long id;
    private String login;
    @Column(name = "username")
    private String name;
    private LocalDate birthdate = LocalDate.of(1900, 1, 1);
    private long balance = 0;

}

