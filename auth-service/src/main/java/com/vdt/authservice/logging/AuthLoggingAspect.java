package com.vdt.authservice.logging;

import com.vdt.authservice.dto.request.auth.LoginRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class AuthLoggingAspect {

    // --- LOGIN ---
    @Pointcut("execution(* com.vdt.authservice.service.AuthService.login(..))")
    public void loginPointcut() {}

    @AfterReturning("loginPointcut()")
    public void logAfterLoginSuccess(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof LoginRequest request) {
            log.info("LOGIN_SUCCESS: Account identifier [{}] logged in successfully.", request.getIdentifier());
        } else {
            log.info("LOGIN_SUCCESS: Account logged in successfully.");
        }
    }

    @AfterThrowing(pointcut = "loginPointcut()", throwing = "e")
    public void logAfterLoginFailure(JoinPoint joinPoint, Exception e) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof LoginRequest request) {
            log.warn("LOGIN_FAILED: Identifier [{}] failed to log in. Reason: {}", request.getIdentifier(), e.getMessage());
        } else {
            log.warn("LOGIN_FAILED: Failed to log in. Reason: {}", e.getMessage());
        }
    }

    // --- LOGOUT ---
    @Pointcut("execution(* com.vdt.authservice.service.AuthService.logout(..))")
    public void logoutPointcut() {}

    @AfterReturning("logoutPointcut()")
    public void logAfterLogout(JoinPoint joinPoint) {
        log.info("LOGOUT_SUCCESS: User logged out successfully and tokens were cleared.");
    }

    @AfterThrowing(pointcut = "logoutPointcut()", throwing = "e")
    public void logAfterLogoutFailure(JoinPoint joinPoint, Exception e) {
        log.warn("LOGOUT_FAILED: Failed to logout. Reason: {}", e.getMessage());
    }

    // --- REFRESH TOKEN ---
    @Pointcut("execution(* com.vdt.authservice.service.AuthService.refreshToken(..))")
    public void refreshTokenPointcut() {}

    @AfterReturning("refreshTokenPointcut()")
    public void logAfterRefreshTokenSuccess(JoinPoint joinPoint) {
        log.info("TOKEN_REFRESH_SUCCESS: Tokens were refreshed successfully.");
    }

    @AfterThrowing(pointcut = "refreshTokenPointcut()", throwing = "e")
    public void logAfterRefreshTokenFailure(JoinPoint joinPoint, Exception e) {
        log.warn("TOKEN_REFRESH_FAILED: Failed to refresh tokens. Reason: {}", e.getMessage());
    }

    // --- FORGOT PASSWORD ---
    @Pointcut("execution(* com.vdt.authservice.service.AuthService.forgotPassword(..))")
    public void forgotPasswordPointcut() {}

    @AfterReturning("forgotPasswordPointcut()")
    public void logAfterForgotPasswordSuccess(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof String email) {
            log.info("FORGOT_PASSWORD_SUCCESS: Reset password email sent to [{}].", email);
        } else {
            log.info("FORGOT_PASSWORD_SUCCESS: Reset password email sent.");
        }
    }

    @AfterThrowing(pointcut = "forgotPasswordPointcut()", throwing = "e")
    public void logAfterForgotPasswordFailure(JoinPoint joinPoint, Exception e) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof String email) {
            log.warn("FORGOT_PASSWORD_FAILED: Failed to process forgot password for email [{}]. Reason: {}", email, e.getMessage());
        } else {
            log.warn("FORGOT_PASSWORD_FAILED: Failed to process forgot password. Reason: {}", e.getMessage());
        }
    }

    // --- RESET PASSWORD ---
    @Pointcut("execution(* com.vdt.authservice.service.AuthService.resetPassword(..))")
    public void resetPasswordPointcut() {}

    @AfterReturning("resetPasswordPointcut()")
    public void logAfterResetPasswordSuccess(JoinPoint joinPoint) {
        log.info("PASSWORD_RESET_SUCCESS: Password was reset successfully.");
    }

    @AfterThrowing(pointcut = "resetPasswordPointcut()", throwing = "e")
    public void logAfterResetPasswordFailure(JoinPoint joinPoint, Exception e) {
        log.warn("PASSWORD_RESET_FAILED: Failed to reset password. Reason: {}", e.getMessage());
    }
}
