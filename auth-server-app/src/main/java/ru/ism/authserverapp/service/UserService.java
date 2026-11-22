package ru.ism.authserverapp.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.ism.authserverapp.client.AccountClient;

@Service
public class UserService implements UserDetailsService {
    private final AccountClient accountClient;

    public UserService(AccountClient accountClient) {
        this.accountClient = accountClient;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return accountClient.getUserForLogin(username);
    }
}
