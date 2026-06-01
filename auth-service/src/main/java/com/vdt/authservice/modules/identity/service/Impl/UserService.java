package com.vdt.authservice.modules.identity.service.Impl;

import com.vdt.authservice.modules.identity.dto.request.user.RegisterRequest;
import com.vdt.authservice.modules.identity.dto.response.user.UserResponse;
import com.vdt.authservice.modules.identity.entity.Account;
import com.vdt.authservice.modules.identity.entity.Role;
import com.vdt.authservice.common.exception.AppException;
import com.vdt.authservice.common.exception.ErrorCode;
import com.vdt.authservice.common.notification.email.IEmailService;
import com.vdt.authservice.modules.identity.mapper.UserMapper;
import com.vdt.authservice.modules.identity.repository.AccountRepository;
import com.vdt.authservice.modules.identity.repository.RoleRepository;
import com.vdt.authservice.modules.identity.security.service.IAccountTokenService;

import com.vdt.authservice.modules.identity.service.IUserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService implements IUserService {
    AccountRepository accountRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    IAccountTokenService accountTokenService;
    IEmailService emailService;
    RoleRepository roleRepository;

    @Transactional
    @Override
    public UserResponse register(RegisterRequest request) {
        if (accountRepository.existsByEmail(request.getEmail()) || accountRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        Set<String> roleNames = request.getRoles();
        Set<Role> roles = roleRepository.findAllByNameIn(roleNames);

        Account account = Account.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .isActive(false)
                .isEmailVerified(false)
                .roles(roles)
                .createdAt(LocalDateTime.now())
                .build();

        account = accountRepository.save(account);
        sendVerificationEmail(account);

        return userMapper.toUserResponse(account);
    }

    @Transactional
    @Override
    public void verifyRegistration(String token) {
        Account account = verifyRegistrationTokenAndGetAccount(token);

        account.setActive(true);
        account.setEmailVerified(true);
        accountRepository.save(account);
        
        accountTokenService.deleteVerificationToken(token);
    }

    @Override
    public void resendVerificationEmail(String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_USED_BY_ANY_ACCOUNT));

        if (account.isEmailVerified()) {
            throw new AppException(ErrorCode.USER_ALREADY_VERIFIED);
        }

        sendVerificationEmail(account);
    }

    private void sendVerificationEmail(Account account) {
        String token = accountTokenService.getExistingVerificationToken(account.getId());
        if (token == null) {
            token = accountTokenService.generateVerificationToken(account.getId());
        }
        emailService.sendVerificationEmail(account.getEmail(), token);
    }

    private Account verifyRegistrationTokenAndGetAccount(String token) {
        String accountId = accountTokenService.getAccountIdByVerificationToken(token);
        if (accountId == null) {
            throw new AppException(ErrorCode.INVALID_ONETIME_TOKEN);
        }

        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));
    }
}
