package com.vdt.authservice.controller;

import com.vdt.authservice.dto.request.user.RegisterRequest;
import com.vdt.authservice.dto.request.user.ResendVerificationRequest;
import com.vdt.authservice.dto.response.ApiResponse;
import com.vdt.authservice.dto.response.user.UserResponse;
import com.vdt.authservice.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping
    public ApiResponse<List<UserResponse>> getAllUsers() {
        return ApiResponse.<List<UserResponse>>builder()
                .result(userService.getAllUsers())
                .build();
    }
}
