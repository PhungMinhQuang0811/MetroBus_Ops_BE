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

        String token = accountTokenService.generateActivationToken(account.getId());
        emailService.sendActivationEmail(account.getEmail(), token);

        return userMapper.toUserResponse(account);
    }
}
