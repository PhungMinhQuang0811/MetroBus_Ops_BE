package com.vdt.afc_ops_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "station_control_syncs",
        uniqueConstraints = @UniqueConstraint(name = "uk_station_control_syncs_station_package", columnNames = {"station_id", "control_package_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StationControlSync {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "station_id", nullable = false, foreignKey = @ForeignKey(name = "fk_station_control_syncs_station"))
    private Station station;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "control_package_id", nullable = false, foreignKey = @ForeignKey(name = "fk_station_control_syncs_control_package"))
    private ControlPackage controlPackage;

    @Column(name = "sync_status", nullable = false, length = 30)
    private String syncStatus;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
