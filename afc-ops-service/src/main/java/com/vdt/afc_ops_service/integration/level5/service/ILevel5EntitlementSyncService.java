package com.vdt.afc_ops_service.integration.level5.service;

import com.vdt.afc_ops_service.integration.level5.dto.message.entitlement.C5EntitlementMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.entitlement.C5EntitlementUnlinkedMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.ticket.C5TicketSyncMessage;
import com.vdt.afc_ops_service.integration.level5.dto.response.Level5BusinessSyncItemResult;

public interface ILevel5EntitlementSyncService {

    Level5BusinessSyncItemResult processEntitlement(C5EntitlementMessage message);

    Level5BusinessSyncItemResult processEntitlementSnapshot(C5TicketSyncMessage message);

    Level5BusinessSyncItemResult processTicketUnlinked(C5EntitlementUnlinkedMessage message);
}
