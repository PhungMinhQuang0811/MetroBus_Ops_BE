package com.vdt.afc_ops_service.integration.level5.service;

import com.vdt.afc_ops_service.integration.level5.dto.message.operator.C5OperatorSyncMessage;
import com.vdt.afc_ops_service.integration.level5.dto.response.Level5BusinessSyncItemResult;

public interface ILevel5OperatorSyncService {

    Level5BusinessSyncItemResult processOperatorSnapshot(C5OperatorSyncMessage message);
}
