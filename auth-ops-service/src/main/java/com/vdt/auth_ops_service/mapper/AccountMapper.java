package com.vdt.auth_ops_service.mapper;

import com.vdt.auth_ops_service.dto.response.account.AccountResponse;
import com.vdt.auth_ops_service.entity.Account;
import com.vdt.auth_ops_service.entity.Role;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AccountMapper {
    public AccountResponse toAccountResponse(Account account) {
        if (account == null) return null;

        Set<String> roles = account.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return AccountResponse.builder()
                .id(account.getId())
                .username(account.getUsername())
                .operatorCode(account.getOperatorCode())
                .roles(roles)
                .isActive(account.isActive())
                .passwordStatus(account.getPasswordStatus())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
