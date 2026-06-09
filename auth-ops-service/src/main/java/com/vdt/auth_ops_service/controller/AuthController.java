package com.vdt.auth_ops_service.controller;

import com.vdt.auth_ops_service.dto.response.ApiResponse;
import com.vdt.auth_ops_service.dto.request.account.RequestPasswordResetRequest;
import com.vdt.auth_ops_service.dto.request.auth.LoginRequest;
import com.vdt.auth_ops_service.dto.response.account.RequestPasswordResetResponse;
import com.vdt.auth_ops_service.dto.response.auth.AuthResponse;
import com.vdt.auth_ops_service.service.IAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthController {
    IAuthService authService;

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse response) {
        return ApiResponse.<AuthResponse>builder()
                .result(authService.login(request, httpRequest, response))
                .build();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return ApiResponse.<Void>builder()
                .build();
    }

    @PostMapping("/refresh-token")
    public ApiResponse<Void> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        authService.refreshToken(request, response);
        return ApiResponse.<Void>builder()
                .build();
    }

    @PostMapping("/forgot-password")
    public ApiResponse<RequestPasswordResetResponse> forgotPassword(
            @Valid @RequestBody RequestPasswordResetRequest request
    ) {
        return ApiResponse.<RequestPasswordResetResponse>builder()
                .result(authService.requestPasswordReset(request))
                .build();
    }
}
