package ru.ism.mybankaccountapp.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ru.ism.mybankaccountapp.model.Account;
import ru.ism.mybankdto.module.AccountRequestDto;
import ru.ism.mybankdto.module.AccountResponseDto;
import ru.ism.mybankdto.module.AccountShortResponse;

@Mapper(componentModel = "spring")
public interface AccountMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "login", ignore = true)
    @Mapping(target = "balance", ignore = true)
    void updateAccount(@MappingTarget Account account, AccountRequestDto accountRequestDto);

    AccountResponseDto toAccountResponseDto(Account account);

    AccountShortResponse toAccountShortResponse(Account account);
}
