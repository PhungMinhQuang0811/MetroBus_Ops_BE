package com.vdt.afc_ops_service.repository;

import com.vdt.afc_ops_service.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, String> {

    Optional<Ticket> findByIdAndCardId(String id, String cardId);

    List<Ticket> findAllByCardIdAndUsageStatusInAndValidToAfter(String cardId,
                                                                Collection<String> usageStatuses,
                                                                LocalDateTime now);
}
