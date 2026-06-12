package com.vdt.afc_ops_service.integration.level5.service;

import com.vdt.afc_ops_service.integration.level5.dto.message.card.C5BlacklistMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.card.C5CardStatusMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.ticket.C5TicketMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.ticket.C5TicketUnlinkedMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.Level5BusinessSyncMessage;
import com.vdt.afc_ops_service.integration.level5.dto.response.Level5BusinessSyncItemResult;
import com.vdt.afc_ops_service.integration.level5.dto.response.Level5BusinessSyncResult;

public interface ILevel5BusinessSyncService {

    Level5BusinessSyncResult processSync(Level5BusinessSyncMessage message);

    Level5BusinessSyncItemResult processC5CardStatus(C5CardStatusMessage message);

    Level5BusinessSyncItemResult processC5Blacklist(String routingKey, C5BlacklistMessage message);

    Level5BusinessSyncItemResult processC5Ticket(C5TicketMessage message);

    Level5BusinessSyncItemResult processC5TicketUnlinked(C5TicketUnlinkedMessage message);
}
