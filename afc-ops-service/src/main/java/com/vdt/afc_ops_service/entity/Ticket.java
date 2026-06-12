package com.vdt.afc_ops_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    @Id
    @Column(length = 100)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", foreignKey = @ForeignKey(name = "fk_tickets_card"))
    private Card card;

    @Column(name = "ticket_type", nullable = false, length = 50)
    private String ticketType;

    @Column(name = "route_scope_type", nullable = false, length = 30)
    private String routeScopeType;

    @Column(name = "operator_ref", length = 100)
    private String operatorRef;

    @Column(name = "route_ref", length = 100)
    private String routeRef;

    @Column(name = "from_station_ref", length = 100)
    private String fromStationRef;

    @Column(name = "to_station_ref", length = 100)
    private String toStationRef;

    @Column(name = "transport_type", nullable = false, length = 30)
    private String transportType;

    @Column(name = "usage_status", nullable = false, length = 30)
    private String usageStatus;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_to", nullable = false)
    private LocalDateTime validTo;

    @Column(name = "first_tap_at")
    private LocalDateTime firstTapAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "source_version", nullable = false)
    private Long sourceVersion;

    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
