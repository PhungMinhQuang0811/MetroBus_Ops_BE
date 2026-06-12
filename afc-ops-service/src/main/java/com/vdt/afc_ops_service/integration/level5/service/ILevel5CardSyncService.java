package com.vdt.afc_ops_service.integration.level5.service;

import com.vdt.afc_ops_service.integration.level5.dto.message.card.C5BlacklistMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.card.C5CardSyncMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.card.C5CardStatusMessage;
import com.vdt.afc_ops_service.integration.level5.dto.response.Level5BusinessSyncItemResult;

public interface ILevel5CardSyncService {

    Level5BusinessSyncItemResult processCardStatus(C5CardStatusMessage message);

    Level5BusinessSyncItemResult processCardSnapshot(C5CardSyncMessage message);

    Level5BusinessSyncItemResult processBlacklist(String routingKey, C5BlacklistMessage message);
}
