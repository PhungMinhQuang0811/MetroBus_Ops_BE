package com.vdt.authservice.modules.identity.service;

import com.vdt.authservice.modules.identity.dto.request.user.RegisterRequest;
import com.vdt.authservice.modules.identity.dto.response.user.UserResponse;

public interface IUserService {
    UserResponse register(RegisterRequest request);
    void verifyRegistration(String token);
    void resendVerificationEmail(String email);
}
