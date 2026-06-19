package com.vdt.auth_ops_service.logging;

import com.vdt.auth_ops_service.common.util.AuditContextUtil;
import com.vdt.auth_ops_service.dto.response.ApiResponse;
import com.vdt.auth_ops_service.dto.response.account.AccountResponse;
import com.vdt.auth_ops_service.dto.response.auth.AuthResponse;
import com.vdt.auth_ops_service.security.util.SecurityUtils;
import com.vdt.auth_ops_service.service.IAuditLogService;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartRequest;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.WebRequest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthAuditAspect {

    IAuditLogService auditLogService;
    ObjectMapper objectMapper;

    @Around("""
            execution(* com.vdt.auth_ops_service.controller.AuthController.login(..)) ||
            execution(* com.vdt.auth_ops_service.controller.AuthController.logout(..)) ||
            execution(* com.vdt.auth_ops_service.controller.AuthController.refreshToken(..)) ||
            execution(* com.vdt.auth_ops_service.controller.AuthController.forgotPassword(..)) ||
            execution(* com.vdt.auth_ops_service.controller.AccountController.createAccount(..)) ||
            execution(* com.vdt.auth_ops_service.controller.AccountController.disableAccount(..)) ||
            execution(* com.vdt.auth_ops_service.controller.AccountController.enableAccount(..)) ||
            execution(* com.vdt.auth_ops_service.controller.AccountController.confirmImportAccounts(..)) ||
            execution(* com.vdt.auth_ops_service.controller.AccountController.changePassword(..)) ||
            execution(* com.vdt.auth_ops_service.controller.AccountController.resetAccountPassword(..))
            """)
    public Object recordAudit(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = ((MethodSignature) joinPoint.getSignature()).getName();
        Object requestBody = extractRequestBody(joinPoint.getArgs());
        Object requestPayload = buildRequestPayload(joinPoint.getArgs());
        try {
            Object result = joinPoint.proceed();
            Object responsePayload = unwrapResult(result);
            String actorId = resolveActorId(methodName, requestBody, responsePayload);
            String actorName = resolveActorName(methodName, requestBody, responsePayload);
            auditLogSafely(methodName, joinPoint.getArgs(), requestBody, requestPayload,
                    responsePayload, actorId, actorName, null);
            return result;
        } catch (Throwable throwable) {
            String actorId = resolveActorId(methodName, requestBody, null);
            String actorName = resolveActorName(methodName, requestBody, null);
            auditLogSafely(methodName, joinPoint.getArgs(), requestBody, requestPayload,
                    null, actorId, actorName, throwable.getMessage());
            throw throwable;
        }
    }

    private void auditLogSafely(String methodName, Object[] args, Object requestBody, Object requestPayload,
                                Object responsePayload, String actorId, String actorName, String errorMessage) {
        try {
            AuditMeta meta = resolveMeta(methodName);
            if (meta == null) {
                return;
            }

            auditLogService.record(
                    meta.action(),
                    meta.resourceType(),
                    resolveResourceId(methodName, args, requestBody, responsePayload),
                    resolveResourceName(methodName, args, requestBody, responsePayload),
                    errorMessage == null ? "SUCCESS" : "FAILED",
                    actorId,
                    actorName,
                    requestPayload,
                    responsePayload,
                    errorMessage
            );
        } catch (Exception exception) {
            log.warn("Failed to persist auth audit log for {}. Reason: {}", methodName, exception.getMessage());
        }
    }

    private AuditMeta resolveMeta(String methodName) {
        return switch (methodName) {
            case "login" -> new AuditMeta("AUTH_LOGIN", "AUTH_SESSION");
            case "logout" -> new AuditMeta("AUTH_LOGOUT", "AUTH_SESSION");
            case "refreshToken" -> new AuditMeta("AUTH_REFRESH_TOKEN", "AUTH_SESSION");
            case "forgotPassword" -> new AuditMeta("AUTH_PASSWORD_RESET_REQUEST", "ACCOUNT");
            case "createAccount" -> new AuditMeta("ACCOUNT_CREATED", "ACCOUNT");
            case "disableAccount" -> new AuditMeta("ACCOUNT_DISABLED", "ACCOUNT");
            case "enableAccount" -> new AuditMeta("ACCOUNT_ENABLED", "ACCOUNT");
            case "confirmImportAccounts" -> new AuditMeta("ACCOUNT_IMPORT_CONFIRMED", "ACCOUNT");
            case "changePassword" -> new AuditMeta("ACCOUNT_PASSWORD_CHANGED", "ACCOUNT");
            case "resetAccountPassword" -> new AuditMeta("ACCOUNT_PASSWORD_RESET", "ACCOUNT");
            default -> null;
        };
    }

    private String resolveActorId(String methodName, Object requestBody, Object responsePayload) {
        String currentAccountId = SecurityUtils.getCurrentAccountId();
        if (currentAccountId != null && !currentAccountId.isBlank()) {
            return currentAccountId;
        }

        String fromResponse = extractStringValue(responsePayload, "getId", "getAccountId");
        if (fromResponse != null) {
            return fromResponse;
        }

        return null;
    }

    private String resolveActorName(String methodName, Object requestBody, Object responsePayload) {
        String currentUsername = SecurityUtils.getCurrentUsername();
        if (currentUsername != null && !currentUsername.isBlank()) {
            return currentUsername;
        }

        String fromResponse = extractStringValue(responsePayload, "getUsername", "getName");
        if (fromResponse != null) {
            return fromResponse;
        }

        if ("login".equals(methodName) || "forgotPassword".equals(methodName)) {
            return extractStringValue(requestBody, "getUsername");
        }
        return extractStringValue(requestBody, "getUsername");
    }

    private String resolveResourceId(String methodName, Object[] args, Object requestBody, Object responsePayload) {
        String fromResponse = extractStringValue(responsePayload,
                "getId", "getAccountId", "getUsername");
        if (fromResponse != null) {
            return fromResponse;
        }

        String fromPath = extractFirstSimpleValue(args);
        if (fromPath != null) {
            return fromPath;
        }

        String fromRequest = extractStringValue(requestBody,
                "getId", "getAccountId", "getUsername");
        if (fromRequest != null) {
            return fromRequest;
        }

        if ("login".equals(methodName) || "forgotPassword".equals(methodName)) {
            return extractStringValue(requestBody, "getUsername");
        }

        String currentAccountId = SecurityUtils.getCurrentAccountId();
        if (currentAccountId != null && !currentAccountId.isBlank()) {
            return currentAccountId;
        }

        return null;
    }

    private String resolveResourceName(String methodName, Object[] args, Object requestBody, Object responsePayload) {
        String fromResponse = extractStringValue(responsePayload,
                "getUsername", "getName", "getAccountId");
        if (fromResponse != null) {
            return fromResponse;
        }

        String fromRequest = extractStringValue(requestBody,
                "getUsername", "getName", "getAccountId");
        if (fromRequest != null) {
            return fromRequest;
        }

        if ("login".equals(methodName) || "forgotPassword".equals(methodName)) {
            return extractStringValue(requestBody, "getUsername");
        }

        String currentUsername = SecurityUtils.getCurrentUsername();
        if (currentUsername != null && !currentUsername.isBlank()) {
            return currentUsername;
        }

        return extractFirstSimpleValue(args);
    }

    private Object unwrapResult(Object result) {
        if (result instanceof ApiResponse<?> apiResponse) {
            return apiResponse.getResult();
        }
        return result;
    }

    private Object buildRequestPayload(Object[] args) {
        List<Object> values = new ArrayList<>();
        for (Object arg : args) {
            Object sanitized = sanitizeArgument(arg);
            if (sanitized != null) {
                values.add(sanitized);
            }
        }
        if (values.isEmpty()) {
            return null;
        }
        return values;
    }

    private Object extractRequestBody(Object[] args) {
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            if (arg instanceof HttpServletRequest || arg instanceof HttpServletResponse
                    || arg instanceof BindingResult || arg instanceof WebRequest
                    || arg instanceof MultipartFile || arg instanceof Collection<?>) {
                continue;
            }
            if (!isSimpleValue(arg)) {
                return arg;
            }
        }
        return null;
    }

    private Object sanitizeArgument(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof HttpServletRequest || value instanceof HttpServletResponse
                || value instanceof BindingResult || value instanceof WebRequest
                || value instanceof ConstraintViolationException) {
            return null;
        }
        if (value instanceof MultipartFile file) {
            return Map.of(
                    "originalFilename", file.getOriginalFilename(),
                    "contentType", file.getContentType(),
                    "size", file.getSize()
            );
        }
        if (value instanceof MultipartRequest request) {
            return Map.of("fileNames", request.getFileMap().keySet());
        }
        if (value instanceof Collection<?> collection) {
            List<Object> items = new ArrayList<>();
            for (Object item : collection) {
                Object sanitized = sanitizeArgument(item);
                if (sanitized != null) {
                    items.add(sanitized);
                }
            }
            return items;
        }
        if (value.getClass().isArray()) {
            Object[] array = (Object[]) value;
            List<Object> items = new ArrayList<>();
            for (Object item : array) {
                Object sanitized = sanitizeArgument(item);
                if (sanitized != null) {
                    items.add(sanitized);
                }
            }
            return items;
        }
        return value;
    }

    private String extractStringValue(Object source, String... getterNames) {
        if (source == null) {
            return null;
        }
        for (String getterName : getterNames) {
            try {
                Method method = source.getClass().getMethod(getterName);
                Object value = method.invoke(source);
                if (value != null) {
                    String stringValue = String.valueOf(value);
                    if (!stringValue.isBlank()) {
                        return stringValue;
                    }
                }
            } catch (Exception ignored) {
                // ignore and continue
            }
        }
        return null;
    }

    private String extractFirstSimpleValue(Object[] args) {
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            if (arg instanceof HttpServletRequest || arg instanceof HttpServletResponse
                    || arg instanceof BindingResult || arg instanceof WebRequest
                    || arg instanceof MultipartFile || arg instanceof Collection<?>) {
                continue;
            }
            if (isSimpleValue(arg)) {
                return String.valueOf(arg);
            }
        }
        return null;
    }

    private boolean isSimpleValue(Object value) {
        return value instanceof String
                || value instanceof Number
                || value instanceof Boolean
                || value.getClass().isEnum();
    }

    private record AuditMeta(String action, String resourceType) {}
}
