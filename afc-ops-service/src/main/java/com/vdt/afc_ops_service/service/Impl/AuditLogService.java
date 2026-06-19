package com.vdt.afc_ops_service.service.Impl;

import tools.jackson.databind.ObjectMapper;
import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import com.vdt.afc_ops_service.common.util.AuditContextUtil;
import com.vdt.afc_ops_service.document.AfcAuditLog;
import com.vdt.afc_ops_service.dto.response.PageResponse;
import com.vdt.afc_ops_service.dto.response.audit.AfcAuditLogResponse;
import com.vdt.afc_ops_service.security.util.SecurityUtils;
import com.vdt.afc_ops_service.service.IAuditLogService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuditLogService implements IAuditLogService {
    MongoTemplate mongoTemplate;
    ObjectMapper objectMapper;

    @Override
    public void record(String action, String resourceType, String resourceId, String resourceName,
                       String result, String actorId, String actorName,
                       Object requestPayload, Object responsePayload, String errorMessage) {
        String resolvedActorId = actorId != null ? actorId : SecurityUtils.getCurrentAccountId();
        String resolvedActorName = actorName != null ? actorName : SecurityUtils.getCurrentUsername();
        String operatorCode = SecurityUtils.getCurrentOperatorCode();

        AfcAuditLog log = AfcAuditLog.builder()
                .operatorCode(operatorCode)
                .accountId(resolvedActorId)
                .username(resolvedActorName)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .resourceName(resourceName)
                .result(result)
                .requestId(AuditContextUtil.getRequestId())
                .ipAddress(AuditContextUtil.getClientIp())
                .userAgent(AuditContextUtil.getUserAgent())
                .httpMethod(AuditContextUtil.getHttpMethod())
                .requestPath(AuditContextUtil.getRequestPath())
                .requestData(toJson(requestPayload))
                .responseData(toJson(responsePayload))
                .errorMessage(errorMessage)
                .createdAt(LocalDateTime.now())
                .build();

        mongoTemplate.save(log);
    }

    @Override
    public PageResponse<AfcAuditLogResponse> search(LocalDateTime from, LocalDateTime to, String username, String action,
                                                    String resourceType, String resourceId, int page, int size) {
        validatePageRequest(page, size);
        AuditTimeRange range = resolveRange(from, to);

        Criteria criteria = Criteria.where("createdAt").gte(range.from()).lte(range.to());
        String currentOperatorCode = SecurityUtils.getCurrentOperatorCode();
        if (currentOperatorCode != null && !currentOperatorCode.isBlank()) {
            criteria = criteria.and("operatorCode").is(currentOperatorCode);
        }
        if (username != null && !username.isBlank()) {
            criteria = criteria.and("username").regex(username, "i");
        }
        if (action != null && !action.isBlank()) {
            criteria = criteria.and("action").is(action);
        }
        if (resourceType != null && !resourceType.isBlank()) {
            criteria = criteria.and("resourceType").is(resourceType);
        }
        if (resourceId != null && !resourceId.isBlank()) {
            criteria = criteria.and("resourceId").is(resourceId);
        }

        Pageable pageable = PageRequest.of(page, size);
        Query query = Query.query(criteria).with(pageable);
        List<AfcAuditLog> logs = mongoTemplate.find(query, AfcAuditLog.class);
        long total = mongoTemplate.count(Query.query(criteria), AfcAuditLog.class);
        List<AfcAuditLogResponse> items = logs.stream().map(this::toResponse).toList();

        return PageResponse.<AfcAuditLogResponse>builder()
                .items(items)
                .page(page)
                .size(size)
                .totalElements(total)
                .totalPages((int) Math.ceil((double) total / size))
                .build();
    }

    @Override
    public AfcAuditLogResponse getById(String id) {
        AfcAuditLog log = mongoTemplate.findById(id, AfcAuditLog.class);
        if (log == null) {
            return null;
        }
        return toResponse(log);
    }

    private AfcAuditLogResponse toResponse(AfcAuditLog log) {
        return AfcAuditLogResponse.builder()
                .id(log.getId())
                .operatorCode(log.getOperatorCode())
                .accountId(log.getAccountId())
                .username(log.getUsername())
                .action(log.getAction())
                .resourceType(log.getResourceType())
                .resourceId(log.getResourceId())
                .resourceName(log.getResourceName())
                .result(log.getResult())
                .requestId(log.getRequestId())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .httpMethod(log.getHttpMethod())
                .requestPath(log.getRequestPath())
                .requestData(log.getRequestData())
                .responseData(log.getResponseData())
                .errorMessage(log.getErrorMessage())
                .createdAt(log.getCreatedAt())
                .build();
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }

        Object normalized = objectMapper.convertValue(value, Object.class);
        Object masked = maskSensitiveFields(normalized);
        return objectMapper.valueToTree(masked).toString();
    }

    private void validatePageRequest(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new AppException(ErrorCode.INVALID_PAGE_REQUEST);
        }
    }

    private AuditTimeRange resolveRange(LocalDateTime from, LocalDateTime to) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime queryFrom = from;
        LocalDateTime queryTo = to;

        if (queryFrom == null && queryTo == null) {
            queryFrom = now.minusHours(24);
            queryTo = now;
        } else if (queryFrom == null) {
            queryFrom = queryTo.minusHours(24);
        } else if (queryTo == null) {
            queryTo = now;
        }

        if (queryFrom.isAfter(queryTo)) {
            throw new AppException(ErrorCode.INVALID_AUDIT_TIME_RANGE);
        }
        return new AuditTimeRange(queryFrom, queryTo);
    }

    private Object maskSensitiveFields(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> masked = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (isSensitiveKey(key)) {
                    masked.put(key, "***");
                } else {
                    masked.put(key, maskSensitiveFields(entry.getValue()));
                }
            }
            return masked;
        }

        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .map(this::maskSensitiveFields)
                    .filter(Objects::nonNull)
                    .toList();
        }

        return value;
    }

    private boolean isSensitiveKey(String key) {
        String normalized = key == null ? "" : key.toLowerCase();
        return normalized.contains("password")
                || normalized.contains("secret")
                || normalized.contains("token")
                || normalized.contains("refresh")
                || normalized.contains("currentpassword")
                || normalized.contains("newpassword")
                || normalized.contains("confirmpassword");
    }

    private record AuditTimeRange(LocalDateTime from, LocalDateTime to) {}
}
