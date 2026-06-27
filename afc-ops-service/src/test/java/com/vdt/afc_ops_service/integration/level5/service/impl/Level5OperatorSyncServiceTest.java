package com.vdt.afc_ops_service.integration.level5.service.impl;

import com.vdt.afc_ops_service.constant.PredefinedMasterDataStatus;
import com.vdt.afc_ops_service.entity.Operator;
import com.vdt.afc_ops_service.integration.level5.constant.PredefinedLevel5BusinessSync;
import com.vdt.afc_ops_service.integration.level5.dto.message.operator.C5OperatorSyncMessage;
import com.vdt.afc_ops_service.integration.level5.dto.response.Level5BusinessSyncItemResult;
import com.vdt.afc_ops_service.repository.OperatorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Level5OperatorSyncServiceTest {

    @Mock
    OperatorRepository operatorRepository;

    Level5OperatorSyncService service;

    @BeforeEach
    void setUp() {
        service = new Level5OperatorSyncService(operatorRepository);
    }

    @Test
    void processOperatorSnapshot_nullMessage_returnsRejected() {
        Level5BusinessSyncItemResult result = service.processOperatorSnapshot(null);
        assertEquals(PredefinedLevel5BusinessSync.REJECTED, result.getResult());
        assertEquals("INVALID_OPERATOR_SYNC_MESSAGE", result.getErrorCode());
    }

    @Test
    void processOperatorSnapshot_nullOrBlankCode_returnsRejected() {
        C5OperatorSyncMessage msg1 = C5OperatorSyncMessage.builder().id(UUID.randomUUID()).code(null).build();
        Level5BusinessSyncItemResult result1 = service.processOperatorSnapshot(msg1);
        assertEquals(PredefinedLevel5BusinessSync.REJECTED, result1.getResult());

        C5OperatorSyncMessage msg2 = C5OperatorSyncMessage.builder().id(UUID.randomUUID()).code("  ").build();
        Level5BusinessSyncItemResult result2 = service.processOperatorSnapshot(msg2);
        assertEquals(PredefinedLevel5BusinessSync.REJECTED, result2.getResult());
    }

    @Test
    void processOperatorSnapshot_newOperator_createsOperator() {
        C5OperatorSyncMessage msg = C5OperatorSyncMessage.builder()
                .id(UUID.randomUUID())
                .code("OP1")
                .name("Operator One")
                .status("ACTIVE")
                .build();

        when(operatorRepository.findByOperatorCode("OP1")).thenReturn(Optional.empty());

        Level5BusinessSyncItemResult result = service.processOperatorSnapshot(msg);
        assertEquals(PredefinedLevel5BusinessSync.CREATED, result.getResult());
        assertEquals("OP1", result.getExternalId());
        verify(operatorRepository).save(org.mockito.Mockito.argThat(op -> 
                "OP1".equals(op.getOperatorCode()) && 
                "Operator One".equals(op.getOperatorName()) && 
                PredefinedMasterDataStatus.ACTIVE.equals(op.getStatus())
        ));
    }

    @Test
    void processOperatorSnapshot_existingOperator_updatesOperator() {
        C5OperatorSyncMessage msg = C5OperatorSyncMessage.builder()
                .id(UUID.randomUUID())
                .code("OP1")
                .name("Operator New Name")
                .status("DISABLED")
                .build();

        Operator existing = Operator.builder().id(100L).operatorCode("OP1").operatorName("Operator One").status("ACTIVE").build();
        when(operatorRepository.findByOperatorCode("OP1")).thenReturn(Optional.of(existing));

        Level5BusinessSyncItemResult result = service.processOperatorSnapshot(msg);
        assertEquals(PredefinedLevel5BusinessSync.UPDATED, result.getResult());
        assertEquals("OP1", result.getExternalId());
        verify(operatorRepository).save(org.mockito.Mockito.argThat(op -> 
                "OP1".equals(op.getOperatorCode()) && 
                "Operator New Name".equals(op.getOperatorName()) && 
                PredefinedMasterDataStatus.DISABLED.equals(op.getStatus())
        ));
    }
}
