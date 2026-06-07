package ru.ism.mybankaccountapp.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import ru.ism.mybankaccountapp.model.Account;
import ru.ism.mybankdto.module.AccountRequestDto;
import ru.ism.mybankdto.module.AccountResponseDto;
import ru.ism.mybankdto.module.AccountShortResponse;

@Mapper(componentModel = "spring")
public interface AccountMapper {
    Account toAccount(AccountRequestDto accountRequestDto);
    void updateAccount(@MappingTarget Account account, AccountRequestDto accountRequestDto);
    AccountResponseDto toAccountResponseDto(Account account);
    AccountShortResponse toAccountShortResponse(Account account);
}
