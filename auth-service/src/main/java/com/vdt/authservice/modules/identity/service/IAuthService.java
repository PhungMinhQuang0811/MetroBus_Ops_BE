package com.vdt.authservice.modules.identity.service;

import com.vdt.authservice.modules.identity.dto.request.auth.LoginRequest;
import com.vdt.authservice.modules.identity.dto.request.auth.PhoneCheckRequest;
import com.vdt.authservice.modules.identity.dto.request.auth.ResetPasswordRequest;
import com.vdt.authservice.modules.identity.dto.request.auth.SetPasswordRequest;
import com.vdt.authservice.modules.identity.dto.request.auth.VerifyOtpRequest;
import com.vdt.authservice.modules.identity.dto.response.auth.AuthResponse;
import com.vdt.authservice.modules.identity.dto.response.auth.PhoneCheckResponse;
import com.vdt.authservice.modules.identity.dto.response.auth.RegistrationOtpResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface IAuthService {
    PhoneCheckResponse checkPhone(PhoneCheckRequest request);
    AuthResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse response);
    RegistrationOtpResponse verifyRegistrationOtp(VerifyOtpRequest request);
    AuthResponse completeRegistration(SetPasswordRequest request, HttpServletResponse response);
    void forgotPassword(String email);
    void resetPassword(ResetPasswordRequest request);
    void logout(HttpServletRequest request, HttpServletResponse response);
    void refreshToken(HttpServletRequest request, HttpServletResponse response);
}
