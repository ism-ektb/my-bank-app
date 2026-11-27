package ru.ism.currencyrateapp.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.ism.currencyrateapp.service.RateService;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CurrencyRateControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RateService rateService;

    @Test
    void euroRateRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/rate/euro"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dollarRateRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/rate/dollar"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void euroRateRequiresUserRole() throws Exception {
        mockMvc.perform(get("/rate/euro")
                        .with(jwt().authorities(new SimpleGrantedAuthority("account_write"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void dollarRateRequiresUserRole() throws Exception {
        mockMvc.perform(get("/rate/dollar")
                        .with(jwt().authorities(new SimpleGrantedAuthority("account_write"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void euroRateAllowsAuthenticatedUser() throws Exception {
        when(rateService.getEuroRate()).thenReturn(1.08f);

        mockMvc.perform(get("/rate/euro")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(content().string("1.08"));

        verify(rateService).getEuroRate();
    }

    @Test
    void dollarRateAllowsAuthenticatedUser() throws Exception {
        when(rateService.getDollarRate()).thenReturn(0.92f);

        mockMvc.perform(get("/rate/dollar")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(content().string("0.92"));

        verify(rateService).getDollarRate();
    }
}
