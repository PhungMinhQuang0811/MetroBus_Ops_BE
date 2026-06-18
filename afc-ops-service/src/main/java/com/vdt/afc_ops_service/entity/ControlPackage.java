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
        name = "control_packages",
        uniqueConstraints = @UniqueConstraint(name = "uk_control_packages_operator_version", columnNames = {"operator_id", "version"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ControlPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operator_id", nullable = false, foreignKey = @ForeignKey(name = "fk_control_packages_operator"))
    private Operator operator;

    @Column(nullable = false)
    private Long version;

    @Column(name = "package_type", nullable = false, length = 50)
    private String packageType;

    @Column(name = "source_type", nullable = false, length = 30)
    private String sourceType;

    @Column(name = "external_package_code", length = 100)
    private String externalPackageCode;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "payload_ref", length = 100)
    private String payloadRef;

    @Column(name = "created_by_account_id", length = 36)
    private String createdByAccountId;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
