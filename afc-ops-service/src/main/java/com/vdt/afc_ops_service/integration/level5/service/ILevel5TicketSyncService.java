package com.vdt.afc_ops_service.integration.level5.service;

import com.vdt.afc_ops_service.integration.level5.dto.message.ticket.C5TicketMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.ticket.C5TicketSyncMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.ticket.C5TicketUnlinkedMessage;
import com.vdt.afc_ops_service.integration.level5.dto.response.Level5BusinessSyncItemResult;

public interface ILevel5TicketSyncService {

    Level5BusinessSyncItemResult processTicket(C5TicketMessage message);

    Level5BusinessSyncItemResult processTicketSnapshot(C5TicketSyncMessage message);

    Level5BusinessSyncItemResult processTicketUnlinked(C5TicketUnlinkedMessage message);
}
