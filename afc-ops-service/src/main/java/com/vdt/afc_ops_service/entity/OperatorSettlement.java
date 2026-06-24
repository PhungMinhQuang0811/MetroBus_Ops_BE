package com.vdt.afc_ops_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "operator_settlements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperatorSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "settlement_id", nullable = false, length = 36)
    private String settlementId;

    @Column(nullable = false, length = 30)
    private String period;

    @Column(name = "operator_code", nullable = false, length = 50)
    private String operatorCode;

    @Column(name = "allocated_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal allocatedAmount;

    @Column(name = "total_km", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalKm;

    @Column(name = "total_trips", nullable = false)
    private Integer totalTrips;

    @Column(name = "km_ratio", precision = 5, scale = 4, nullable = false)
    private BigDecimal kmRatio;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}