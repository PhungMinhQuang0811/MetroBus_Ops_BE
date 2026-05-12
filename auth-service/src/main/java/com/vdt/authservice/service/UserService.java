package com.vdt.authservice.service;

import com.vdt.authservice.dto.request.user.RegisterRequest;
import com.vdt.authservice.dto.response.user.UserResponse;
import com.vdt.authservice.entity.Account;
import com.vdt.authservice.entity.Role;
import com.vdt.authservice.exception.AppException;
import com.vdt.authservice.exception.ErrorCode;
import com.vdt.authservice.external.notification.email.EmailService;
import com.vdt.authservice.mapper.UserMapper;
import com.vdt.authservice.repository.AccountRepository;
import com.vdt.authservice.repository.RoleRepository;
import com.vdt.authservice.security.service.AccountTokenService;
import com.vdt.authservice.constant.PredefinedRole;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService {
    AccountRepository accountRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    AccountTokenService accountTokenService;
    EmailService emailService;
    RoleRepository roleRepository;

    @Transactional
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
    public void verifyRegistration(String token) {
        Account account = verifyRegistrationTokenAndGetAccount(token);

        account.setActive(true);
        account.setEmailVerified(true);
        accountRepository.save(account);
        
        accountTokenService.deleteVerificationToken(token);
    }

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
