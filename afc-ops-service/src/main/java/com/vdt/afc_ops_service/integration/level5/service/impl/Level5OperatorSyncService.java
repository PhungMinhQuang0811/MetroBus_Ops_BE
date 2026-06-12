package com.vdt.afc_ops_service.integration.level5.service.impl;

import com.vdt.afc_ops_service.constant.PredefinedMasterDataStatus;
import com.vdt.afc_ops_service.entity.Operator;
import com.vdt.afc_ops_service.integration.level5.constant.PredefinedLevel5BusinessSync;
import com.vdt.afc_ops_service.integration.level5.dto.message.operator.C5OperatorSyncMessage;
import com.vdt.afc_ops_service.integration.level5.dto.response.Level5BusinessSyncItemResult;
import com.vdt.afc_ops_service.integration.level5.service.ILevel5OperatorSyncService;
import com.vdt.afc_ops_service.repository.OperatorRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Level5OperatorSyncService implements ILevel5OperatorSyncService {

    OperatorRepository operatorRepository;

    @Override
    @Transactional
    public Level5BusinessSyncItemResult processOperatorSnapshot(C5OperatorSyncMessage message) {
        String operatorCode = normalizeUppercase(message == null ? null : message.getCode());
        if (operatorCode == null) {
            return rejected(null, "INVALID_OPERATOR_SYNC_MESSAGE", "code is required");
        }

        Optional<Operator> existingOperator = operatorRepository.findByOperatorCode(operatorCode);
        Operator operator = existingOperator.orElseGet(Operator::new);
        operator.setOperatorCode(operatorCode);
        operator.setOperatorName(normalize(message.getName()) == null ? operatorCode : normalize(message.getName()));
        operator.setStatus(mapC5OperatorStatus(message.getStatus()));
        operatorRepository.save(operator);

        return success(operatorCode,
                existingOperator.isPresent() ? PredefinedLevel5BusinessSync.UPDATED : PredefinedLevel5BusinessSync.CREATED);
    }

    private String mapC5OperatorStatus(String status) {
        String normalizedStatus = normalizeUppercase(status);
        return PredefinedLevel5BusinessSync.ACTIVE.equals(normalizedStatus)
                ? PredefinedMasterDataStatus.ACTIVE
                : PredefinedMasterDataStatus.DISABLED;
    }

    private Level5BusinessSyncItemResult success(String externalId, String result) {
        return Level5BusinessSyncItemResult.builder()
                .externalId(externalId)
                .result(result)
                .build();
    }

    private Level5BusinessSyncItemResult rejected(String externalId, String errorCode, String message) {
        return Level5BusinessSyncItemResult.builder()
                .externalId(externalId)
                .result(PredefinedLevel5BusinessSync.REJECTED)
                .errorCode(errorCode)
                .message(message)
                .build();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeUppercase(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toUpperCase();
    }
}
