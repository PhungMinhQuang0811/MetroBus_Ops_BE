package com.vdt.authservice.modules.identity.logging;

import com.vdt.authservice.modules.identity.dto.request.user.RegisterRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class UserLoggingAspect {

    // --- REGISTER ---
    @Pointcut("execution(* com.vdt.authservice.modules.identity.service.Impl.UserService.register(..))")
    public void registerPointcut() {}

    @AfterReturning("registerPointcut()")
    public void logAfterRegisterSuccess(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof RegisterRequest request) {
            log.info("USER_REGISTER_SUCCESS: Account registered successfully for email [{}] and username [{}].", request.getEmail(), request.getUsername());
        } else {
            log.info("USER_REGISTER_SUCCESS: Account registered successfully.");
        }
    }

    @AfterThrowing(pointcut = "registerPointcut()", throwing = "e")
    public void logAfterRegisterFailure(JoinPoint joinPoint, Exception e) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof RegisterRequest request) {
            log.warn("USER_REGISTER_FAILED: Failed to register account for email [{}] and username [{}]. Reason: {}", request.getEmail(), request.getUsername(), e.getMessage());
        } else {
            log.warn("USER_REGISTER_FAILED: Failed to register account. Reason: {}", e.getMessage());
        }
    }

    // --- VERIFY REGISTRATION ---
    @Pointcut("execution(* com.vdt.authservice.modules.identity.service.Impl.UserService.verifyRegistration(..))")
    public void verifyRegistrationPointcut() {}

    @AfterReturning("verifyRegistrationPointcut()")
    public void logAfterVerifyRegistrationSuccess(JoinPoint joinPoint) {
        log.info("USER_VERIFY_SUCCESS: Account verified successfully.");
    }

    @AfterThrowing(pointcut = "verifyRegistrationPointcut()", throwing = "e")
    public void logAfterVerifyRegistrationFailure(JoinPoint joinPoint, Exception e) {
        log.warn("USER_VERIFY_FAILED: Failed to verify account registration. Reason: {}", e.getMessage());
    }

    // --- RESEND VERIFICATION EMAIL ---
    @Pointcut("execution(* com.vdt.authservice.modules.identity.service.Impl.UserService.resendVerificationEmail(..))")
    public void resendVerificationEmailPointcut() {}

    @AfterReturning("resendVerificationEmailPointcut()")
    public void logAfterResendVerificationEmailSuccess(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof String email) {
            log.info("USER_RESEND_VERIFY_SUCCESS: Verification email resent successfully to [{}].", email);
        } else {
            log.info("USER_RESEND_VERIFY_SUCCESS: Verification email resent successfully.");
        }
    }

    @AfterThrowing(pointcut = "resendVerificationEmailPointcut()", throwing = "e")
    public void logAfterResendVerificationEmailFailure(JoinPoint joinPoint, Exception e) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof String email) {
            log.warn("USER_RESEND_VERIFY_FAILED: Failed to resend verification email to [{}]. Reason: {}", email, e.getMessage());
        } else {
            log.warn("USER_RESEND_VERIFY_FAILED: Failed to resend verification email. Reason: {}", e.getMessage());
        }
    }
}
