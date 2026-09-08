package ru.ism.mybankaccountapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Hooks;

@SpringBootApplication
public class MyBankAccountAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyBankAccountAppApplication.class, args);
    }

}
