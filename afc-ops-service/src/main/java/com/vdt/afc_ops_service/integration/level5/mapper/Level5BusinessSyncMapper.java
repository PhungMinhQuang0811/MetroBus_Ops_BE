package com.vdt.afc_ops_service.integration.level5.mapper;

import com.vdt.afc_ops_service.integration.level5.dto.message.Level5CardPayload;
import com.vdt.afc_ops_service.integration.level5.dto.message.Level5TicketPayload;
import com.vdt.afc_ops_service.entity.Card;
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
        ticket.setType(payload.getTicketType());
        ticket.setScope(payload.getScope());
        ticket.setMode(payload.getMode());
        ticket.setOperatorRef(payload.getOperatorRef());
        ticket.setRouteRef(payload.getRouteRef());
        ticket.setFromStationRef(payload.getFromStationRef());
        ticket.setToStationRef(payload.getToStationRef());
        ticket.setPrice(payload.getPrice());
        ticket.setUsageStatus(payload.getUsageStatus());
        ticket.setValidFrom(payload.getValidFrom());
        ticket.setValidTo(payload.getValidTo());
        ticket.setFirstTapAt(payload.getFirstTapAt());
        ticket.setUsedAt(payload.getUsedAt());
        ticket.setSourceVersion(sourceVersion);
        ticket.setSyncedAt(syncedAt);
    }
}