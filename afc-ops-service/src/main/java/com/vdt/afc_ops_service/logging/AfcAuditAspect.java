package com.vdt.afc_ops_service.logging;

import tools.jackson.databind.ObjectMapper;
import com.vdt.afc_ops_service.dto.response.ApiResponse;
import com.vdt.afc_ops_service.security.util.SecurityUtils;
import com.vdt.afc_ops_service.service.IAuditLogService;
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
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartRequest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AfcAuditAspect {

    IAuditLogService auditLogService;
    ObjectMapper objectMapper;

    @Around("""
            execution(* com.vdt.afc_ops_service.controller.RouteController.createRoute(..)) ||
            execution(* com.vdt.afc_ops_service.controller.RouteController.updateRoute(..)) ||
            execution(* com.vdt.afc_ops_service.controller.RouteController.enableRoute(..)) ||
            execution(* com.vdt.afc_ops_service.controller.RouteController.disableRoute(..)) ||
            execution(* com.vdt.afc_ops_service.controller.RouteController.confirmImportRoutes(..)) ||
            execution(* com.vdt.afc_ops_service.controller.StationController.createStation(..)) ||
            execution(* com.vdt.afc_ops_service.controller.StationController.updateStation(..)) ||
            execution(* com.vdt.afc_ops_service.controller.StationController.enableStation(..)) ||
            execution(* com.vdt.afc_ops_service.controller.StationController.disableStation(..)) ||
            execution(* com.vdt.afc_ops_service.controller.StationController.confirmImportStations(..)) ||
            execution(* com.vdt.afc_ops_service.controller.DeviceController.createDevice(..)) ||
            execution(* com.vdt.afc_ops_service.controller.DeviceController.updateDevice(..)) ||
            execution(* com.vdt.afc_ops_service.controller.DeviceController.enableDevice(..)) ||
            execution(* com.vdt.afc_ops_service.controller.DeviceController.disableDevice(..)) ||
            execution(* com.vdt.afc_ops_service.controller.DeviceController.confirmImportDevices(..)) ||
            execution(* com.vdt.afc_ops_service.controller.BatchController.createBatch(..)) ||
            execution(* com.vdt.afc_ops_service.controller.AfcOpsControlPackageController.create(..)) ||
            execution(* com.vdt.afc_ops_service.controller.AfcOpsControlPackageController.update(..)) ||
            execution(* com.vdt.afc_ops_service.controller.AfcOpsControlPackageController.publish(..)) ||
            execution(* com.vdt.afc_ops_service.controller.AfcOpsControlPackageController.ackApply(..))
            """)
    public Object recordAudit(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = ((MethodSignature) joinPoint.getSignature()).getName();
        Object[] args = joinPoint.getArgs();
        Object requestBody = extractRequestBody(args);
        Object requestPayload = buildRequestPayload(args);

        String actorId = SecurityUtils.getCurrentAccountId();
        String actorName = SecurityUtils.getCurrentUsername();

        try {
            Object result = joinPoint.proceed();
            Object responsePayload = unwrapResult(result);
            auditLogSafely(methodName, args, requestBody, requestPayload, responsePayload, actorId, actorName, null);
            return result;
        } catch (Throwable throwable) {
            auditLogSafely(methodName, args, requestBody, requestPayload, null, actorId, actorName, throwable.getMessage());
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
            log.warn("Failed to persist AFC audit log for {}. Reason: {}", methodName, exception.getMessage());
        }
    }

    private AuditMeta resolveMeta(String methodName) {
        return switch (methodName) {
            case "createRoute" -> new AuditMeta("ROUTE_CREATED", "ROUTE");
            case "updateRoute" -> new AuditMeta("ROUTE_UPDATED", "ROUTE");
            case "enableRoute" -> new AuditMeta("ROUTE_ENABLED", "ROUTE");
            case "disableRoute" -> new AuditMeta("ROUTE_DISABLED", "ROUTE");
            case "confirmImportRoutes" -> new AuditMeta("ROUTE_IMPORT_CONFIRMED", "ROUTE");
            case "createStation" -> new AuditMeta("STATION_CREATED", "STATION");
            case "updateStation" -> new AuditMeta("STATION_UPDATED", "STATION");
            case "enableStation" -> new AuditMeta("STATION_ENABLED", "STATION");
            case "disableStation" -> new AuditMeta("STATION_DISABLED", "STATION");
            case "confirmImportStations" -> new AuditMeta("STATION_IMPORT_CONFIRMED", "STATION");
            case "createDevice" -> new AuditMeta("DEVICE_CREATED", "DEVICE");
            case "updateDevice" -> new AuditMeta("DEVICE_UPDATED", "DEVICE");
            case "enableDevice" -> new AuditMeta("DEVICE_ENABLED", "DEVICE");
            case "disableDevice" -> new AuditMeta("DEVICE_DISABLED", "DEVICE");
            case "confirmImportDevices" -> new AuditMeta("DEVICE_IMPORT_CONFIRMED", "DEVICE");
            case "createBatch" -> new AuditMeta("BATCH_CREATED", "BATCH");
            case "create" -> new AuditMeta("CONTROL_PACKAGE_CREATED", "CONTROL_PACKAGE");
            case "update" -> new AuditMeta("CONTROL_PACKAGE_UPDATED", "CONTROL_PACKAGE");
            case "publish" -> new AuditMeta("CONTROL_PACKAGE_PUBLISHED", "CONTROL_PACKAGE");
            case "ackApply" -> new AuditMeta("CONTROL_PACKAGE_ACK_APPLIED", "CONTROL_PACKAGE_SYNC");
            default -> null;
        };
    }

    private String resolveResourceId(String methodName, Object[] args, Object requestBody, Object responsePayload) {
        String fromResponse = extractStringValue(responsePayload,
                "getId", "getRouteId", "getStationId", "getDeviceId", "getPackageId",
                "getSyncId", "getBatchId");
        if (fromResponse != null) {
            return fromResponse;
        }

        String fromPath = extractFirstSimpleValue(args);
        if (fromPath != null) {
            return fromPath;
        }

        String fromRequest = extractStringValue(requestBody,
                "getId", "getRouteId", "getStationId", "getDeviceId", "getPackageId",
                "getSyncId", "getBatchId");
        if (fromRequest != null) {
            return fromRequest;
        }

        return null;
    }

    private String resolveResourceName(String methodName, Object[] args, Object requestBody, Object responsePayload) {
        String fromResponse = extractStringValue(responsePayload,
                "getRouteCode", "getStationCode", "getDeviceCode", "getBatchCode",
                "getPackageType", "getStatus", "getRouteName", "getStationName", "getDeviceType");
        if (fromResponse != null) {
            return fromResponse;
        }

        String fromRequest = extractStringValue(requestBody,
                "getRouteName", "getStationName", "getDeviceType", "getPackageType", "getStatus");
        if (fromRequest != null) {
            return fromRequest;
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
