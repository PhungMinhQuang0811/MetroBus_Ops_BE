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
@Table(name = "entitlements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Entitlement {

    @Id
    @Column(length = 100)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false, foreignKey = @ForeignKey(name = "fk_entitlements_card"))
    private Card card;

    @Column(name = "fare_product_code", nullable = false, length = 100)
    private String fareProductCode;

    @Column(name = "pass_period", nullable = false, length = 30)
    private String passPeriod;

    @Column(name = "pass_scope", nullable = false, length = 30)
    private String passScope;

    @Column(name = "operator_ref", nullable = false, length = 100)
    private String operatorRef;

    @Column(name = "route_ref", nullable = false, length = 100)
    private String routeRef;

    @Column(name = "transport_type", nullable = false, length = 30)
    private String transportType;

    @Column(name = "passenger_type", length = 50)
    private String passengerType;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_to", nullable = false)
    private LocalDateTime validTo;

    @Column(name = "source_version", nullable = false)
    private Long sourceVersion;

    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
