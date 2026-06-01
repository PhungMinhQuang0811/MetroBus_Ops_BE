package com.vdt.authservice.modules.identity.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(unique = true, length = 100)
    private String email;

    @Column(unique = true, length = 50)
    private String username;

    @Column(length = 100)
    private String password;

    @Column(name = "phone_number", unique = true, length = 10)
    private String phoneNumber;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column()
    private String address;

    @Column(name = "personal_id", length = 12)
    private String personalId;

    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = false;

    @Column(name = "is_email_verified")
    @Builder.Default
    private boolean isEmailVerified = false;

    @Column(name = "is_phone_verified")
    @Builder.Default
    private boolean isPhoneVerified = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToMany()
    @JoinTable(
            name = "account_roles",
            joinColumns = @JoinColumn(name = "account_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();
}
