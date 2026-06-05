package com.vdt.auth_ops_service.service;

import com.vdt.auth_ops_service.dto.request.auth.LoginRequest;
import com.vdt.auth_ops_service.dto.response.auth.AuthResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface IAuthService {
    AuthResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse response);
    void logout(HttpServletRequest request, HttpServletResponse response);
    void refreshToken(HttpServletRequest request, HttpServletResponse response);
}
