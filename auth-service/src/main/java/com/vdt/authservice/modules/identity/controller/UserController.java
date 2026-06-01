package com.vdt.authservice.modules.identity.controller;

import com.vdt.authservice.common.ApiResponse;
import com.vdt.authservice.modules.identity.dto.request.user.RegisterRequest;
import com.vdt.authservice.modules.identity.dto.request.user.ResendVerificationRequest;
import com.vdt.authservice.modules.identity.dto.response.user.UserResponse;
import com.vdt.authservice.modules.identity.service.Impl.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {
    UserService userService;

    @PostMapping("/register")
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.register(request))
                .build();
    }

    @GetMapping("/verify-registration")
    public ApiResponse<Void> verifyRegistration(@RequestParam String token) {
        userService.verifyRegistration(token);
        return ApiResponse.<Void>builder()
                .build();
    }

    @PostMapping("/resend-verification")
    public ApiResponse<Void> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        userService.resendVerificationEmail(request.getEmail());
        return ApiResponse.<Void>builder()
                .build();
    }
}
