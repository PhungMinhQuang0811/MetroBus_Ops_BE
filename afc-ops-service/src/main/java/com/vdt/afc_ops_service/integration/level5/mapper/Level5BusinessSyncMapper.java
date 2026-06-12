package com.vdt.afc_ops_service.integration.level5.mapper;

import com.vdt.afc_ops_service.integration.level5.dto.message.Level5CardPayload;
import com.vdt.afc_ops_service.integration.level5.dto.message.Level5EntitlementPayload;
import com.vdt.afc_ops_service.integration.level5.dto.message.Level5TicketPayload;
import com.vdt.afc_ops_service.entity.Card;
import com.vdt.afc_ops_service.entity.Entitlement;
import com.vdt.afc_ops_service.entity.Ticket;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class Level5BusinessSyncMapper {

    public Card toCard(Level5CardPayload payload, Long sourceVersion, LocalDateTime syncedAt) {
        Card card = Card.builder().id(payload.getCardId()).build();
        updateCard(card, payload, sourceVersion, syncedAt);
        return card;
    }

    public void updateCard(Card card, Level5CardPayload payload, Long sourceVersion, LocalDateTime syncedAt) {
        card.setCardUid(payload.getCardUid());
        card.setCardType(payload.getCardType());
        card.setStatus(payload.getStatus());
        card.setStatusReason(payload.getStatusReason());
        card.setSourceVersion(sourceVersion);
        card.setSyncedAt(syncedAt);
    }

    public Ticket toTicket(Level5TicketPayload payload, Card card, Long sourceVersion, LocalDateTime syncedAt) {
        Ticket ticket = Ticket.builder().id(payload.getTicketId()).card(card).build();
        updateTicket(ticket, payload, card, sourceVersion, syncedAt);
        return ticket;
    }

    public void updateTicket(Ticket ticket, Level5TicketPayload payload, Card card,
                             Long sourceVersion, LocalDateTime syncedAt) {
        ticket.setCard(card);
        ticket.setTicketType(payload.getTicketType());
        ticket.setRouteScopeType(payload.getRouteScopeType());
        ticket.setOperatorRef(payload.getOperatorRef());
        ticket.setRouteRef(payload.getRouteRef());
        ticket.setTransportType(payload.getTransportType());
        ticket.setUsageStatus(payload.getUsageStatus());
        ticket.setValidFrom(payload.getValidFrom());
        ticket.setValidTo(payload.getValidTo());
        ticket.setFirstTapAt(payload.getFirstTapAt());
        ticket.setUsedAt(payload.getUsedAt());
        ticket.setSourceVersion(sourceVersion);
        ticket.setSyncedAt(syncedAt);
    }

    public Entitlement toEntitlement(Level5EntitlementPayload payload, Card card,
                                     Long sourceVersion, LocalDateTime syncedAt) {
        Entitlement entitlement = Entitlement.builder().id(payload.getEntitlementId()).card(card).build();
        updateEntitlement(entitlement, payload, card, sourceVersion, syncedAt);
        return entitlement;
    }

    public void updateEntitlement(Entitlement entitlement, Level5EntitlementPayload payload, Card card,
                                  Long sourceVersion, LocalDateTime syncedAt) {
        entitlement.setCard(card);
        entitlement.setFareProductCode(payload.getFareProductCode());
        entitlement.setPassPeriod(payload.getPassPeriod());
        entitlement.setPassScope(payload.getPassScope());
        entitlement.setOperatorRef(payload.getOperatorRef());
        entitlement.setRouteRef(payload.getRouteRef());
        entitlement.setTransportType(payload.getTransportType());
        entitlement.setPassengerType(payload.getPassengerType());
        entitlement.setStatus(payload.getStatus());
        entitlement.setValidFrom(payload.getValidFrom());
        entitlement.setValidTo(payload.getValidTo());
        entitlement.setSourceVersion(sourceVersion);
        entitlement.setSyncedAt(syncedAt);
    }
}
