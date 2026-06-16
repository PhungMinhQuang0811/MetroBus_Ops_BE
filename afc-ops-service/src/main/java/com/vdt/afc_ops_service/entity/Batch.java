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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "batches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Batch {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operator_id", nullable = false, foreignKey = @ForeignKey(name = "fk_batches_operator"))
    private Operator operator;

    @Column(name = "batch_code", nullable = false, unique = true, length = 100)
    private String batchCode;

    @Column(name = "from_time", nullable = false)
    private LocalDateTime fromTime;

    @Column(name = "to_time", nullable = false)
    private LocalDateTime toTime;

    @Column(name = "transaction_count", nullable = false)
    private int transactionCount;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "exchange_log_ref", length = 100)
    private String exchangeLogRef;

    @Column(name = "created_by_account_id", length = 36)
    private String createdByAccountId;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
