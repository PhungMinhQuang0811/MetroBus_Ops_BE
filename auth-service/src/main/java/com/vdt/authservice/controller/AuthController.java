package com.vdt.authservice.controller;

import com.vdt.authservice.dto.request.auth.ForgotPasswordRequest;
import com.vdt.authservice.dto.request.auth.LoginRequest;
import com.vdt.authservice.dto.request.auth.ResetPasswordRequest;
import com.vdt.authservice.dto.response.auth.AuthResponse;
import com.vdt.authservice.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import com.vdt.authservice.exception.ErrorCode;
import org.springframework.web.bind.annotation.*;

import com.vdt.authservice.dto.response.ApiResponse;


@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthController {
    AuthService authService;

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse response) {
        return ApiResponse.<AuthResponse>builder()
                .result(authService.login(request, httpRequest, response))
                .build();
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return ApiResponse.<Void>builder()
                .build();
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.<Void>builder()
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
}
