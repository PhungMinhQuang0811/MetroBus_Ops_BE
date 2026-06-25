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

import java.math.BigDecimal;
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

    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(name = "ticket_type", nullable = false, length = 30)
    private String type;

    @Column(name = "operator_ref", length = 100)
    private String operatorRef;

    @Column(name = "route_ref", length = 100)
    private String routeRef;

    @Column(name = "from_station_ref", length = 100)
    private String fromStationRef;

    @Column(name = "to_station_ref", length = 100)
    private String toStationRef;

    @Column(length = 30)
    private String scope;

    @Column(length = 30)
    private String mode;

    @Column(precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "fare_rule_id", length = 36)
    private String fareRuleId;

    @Column(name = "discount_id", length = 36)
    private String discountId;

    @Column(name = "passenger_type", length = 50)
    private String passengerType;

    @Column(name = "usage_status", nullable = false, length = 30)
    private String usageStatus;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_to", nullable = false)
    private LocalDateTime validTo;

    @Column(name = "purchased_at")
    private LocalDateTime purchasedAt;

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