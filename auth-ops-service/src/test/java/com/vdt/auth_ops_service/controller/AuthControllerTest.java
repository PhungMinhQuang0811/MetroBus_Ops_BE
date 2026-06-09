package com.vdt.auth_ops_service.controller;

import com.vdt.auth_ops_service.dto.request.account.RequestPasswordResetRequest;
import com.vdt.auth_ops_service.dto.request.auth.LoginRequest;
import com.vdt.auth_ops_service.dto.response.account.RequestPasswordResetResponse;
import com.vdt.auth_ops_service.dto.response.auth.AuthResponse;
import com.vdt.auth_ops_service.service.IAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    @Mock
    private IAuthService authService;
    @Mock
    private HttpServletRequest httpRequest;
    @Mock
    private HttpServletResponse httpResponse;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(authService);
    }

    @Test
    void login_DelegatesToService() {
        LoginRequest request = LoginRequest.builder()
                .username("operator")
                .password("Password123")
                .build();
        AuthResponse expected = AuthResponse.builder()
                .id("account-id")
                .username("operator")
                .roles(Set.of("STATION_OPERATOR"))
                .permissions(Set.of("ACCOUNT_READ"))
                .build();
        when(authService.login(request, httpRequest, httpResponse)).thenReturn(expected);

        assertSame(expected, controller.login(request, httpRequest, httpResponse).getResult());
    }

    @Test
    void logout_DelegatesToService() {
        assertNull(controller.logout(httpRequest, httpResponse).getResult());

        verify(authService).logout(httpRequest, httpResponse);
    }

    @Test
    void refreshToken_DelegatesToService() {
        assertNull(controller.refreshToken(httpRequest, httpResponse).getResult());

        verify(authService).refreshToken(httpRequest, httpResponse);
    }

    @Test
    void forgotPassword_DelegatesToAccountService() {
        RequestPasswordResetRequest request = RequestPasswordResetRequest.builder()
                .username("station01")
                .build();
        RequestPasswordResetResponse expected = RequestPasswordResetResponse.builder()
                .username("station01")
                .passwordStatus("NEED_TO_RESET")
                .build();
        when(authService.requestPasswordReset(request)).thenReturn(expected);

        assertSame(expected, controller.forgotPassword(request).getResult());
    }
}
