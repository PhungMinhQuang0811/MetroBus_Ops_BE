package com.vdt.auth_ops_service.logging;

import com.vdt.auth_ops_service.dto.request.auth.LoginRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class AuthLoggingAspect {

    // --- LOGIN ---
    @Pointcut("execution(* com.vdt.auth_ops_service.service.Impl.AuthService.login(..))")
    public void loginPointcut() {}

    @AfterReturning("loginPointcut()")
    public void logAfterLoginSuccess(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof LoginRequest request) {
            log.info("LOGIN_SUCCESS: Account identifier [{}] logged in successfully.", request.getUsername());
        } else {
            log.info("LOGIN_SUCCESS: Account logged in successfully.");
        }
    }

    @AfterThrowing(pointcut = "loginPointcut()", throwing = "e")
    public void logAfterLoginFailure(JoinPoint joinPoint, Exception e) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof LoginRequest request) {
            log.warn("LOGIN_FAILED: Identifier [{}] failed to log in. Reason: {}", request.getUsername(), e.getMessage());
        } else {
            log.warn("LOGIN_FAILED: Failed to log in. Reason: {}", e.getMessage());
        }
    }

    // --- LOGOUT ---
    @Pointcut("execution(* com.vdt.auth_ops_service.service.Impl.AuthService.logout(..))")
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
    @Pointcut("execution(* com.vdt.auth_ops_service.service.Impl.AuthService.refreshToken(..))")
    public void refreshTokenPointcut() {}

    @AfterReturning("refreshTokenPointcut()")
    public void logAfterRefreshTokenSuccess(JoinPoint joinPoint) {
        log.info("TOKEN_REFRESH_SUCCESS: Tokens were refreshed successfully.");
    }

    @AfterThrowing(pointcut = "refreshTokenPointcut()", throwing = "e")
    public void logAfterRefreshTokenFailure(JoinPoint joinPoint, Exception e) {
        log.warn("TOKEN_REFRESH_FAILED: Failed to refresh tokens. Reason: {}", e.getMessage());
    }

}
