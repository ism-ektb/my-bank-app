package ru.ism.mybankaccountapp.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.ism.mybankaccountapp.AccountRepository;
import ru.ism.mybankaccountapp.mapper.AccountMapper;
import ru.ism.mybankaccountapp.model.Account;
import ru.ism.mybankaccountapp.model.AccountRequestDto;
import ru.ism.mybankaccountapp.model.AccountResponseDto;
import ru.ism.mybankaccountapp.model.ChangeAccountRequestDto;
import ru.ism.mybankaccountapp.service.AccountService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;


    @Override
    public AccountResponseDto updateAccount(AccountRequestDto accountRequestDto, JwtAuthenticationToken authentication) {
        String login = authentication.getToken().getClaimAsString("preferred_username");
        Optional<Account> account = accountRepository.findByLogin(login);
        if (account.isPresent()) {
            Account ac = account.get();
            accountMapper.updateAccount(ac, accountRequestDto);
            return accountMapper.toAccountResponseDto(accountRepository.save(ac));
        } else {

            return accountMapper.toAccountResponseDto(accountRepository.save(accountMapper.toAccount(accountRequestDto)));
        }


    }

    @Override
    public AccountResponseDto findAccount(JwtAuthenticationToken authentication) {
        String login = authentication.getToken().getClaimAsString("preferred_username");

        Optional<Account> account = accountRepository.findByLogin(login);
        if (account.isPresent()) {
            return accountMapper.toAccountResponseDto(account.get());
        } else {
            Account newAccount = new Account();
            newAccount.setLogin(login);

            return accountMapper.toAccountResponseDto(accountRepository.save(newAccount));
        }
    }


}
