package com.vdt.authservice.modules.identity.controller;

import com.vdt.authservice.common.ApiResponse;
import com.vdt.authservice.modules.identity.dto.request.auth.ForgotPasswordRequest;
import com.vdt.authservice.modules.identity.dto.request.auth.LoginRequest;
import com.vdt.authservice.modules.identity.dto.request.auth.OtpRequest;
import com.vdt.authservice.modules.identity.dto.request.auth.ResetPasswordRequest;
import com.vdt.authservice.modules.identity.dto.request.auth.VerifyOtpRequest;
import com.vdt.authservice.modules.identity.dto.response.auth.AuthResponse;
import com.vdt.authservice.modules.identity.service.IAuthService;
import com.vdt.authservice.modules.identity.service.IOtpService;
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
    IOtpService otpService;

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse response) {
        return ApiResponse.<AuthResponse>builder()
                .result(authService.login(request, httpRequest, response))
                .build();
    }

    @PostMapping("/request-otp")
    public ApiResponse<Void> requestOtp(@Valid @RequestBody OtpRequest request) {
        otpService.requestOtp(request.getPhoneNumber());
        return ApiResponse.<Void>builder()
                .build();
    }

    @PostMapping("/verify-otp")
    public ApiResponse<AuthResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request, HttpServletResponse response) {
        return ApiResponse.<AuthResponse>builder()
                .result(authService.verifyOtp(request, response))
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
