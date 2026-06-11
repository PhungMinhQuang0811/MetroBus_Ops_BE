package com.vdt.afc_ops_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card {

    @Id
    @Column(length = 100)
    private String id;

    @Column(name = "external_user_id", length = 100)
    private String externalUserId;

    @Column(name = "card_type", nullable = false, length = 30)
    private String cardType;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "status_reason", length = 100)
    private String statusReason;

    @Column(name = "source_version", nullable = false)
    private Long sourceVersion;

    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
