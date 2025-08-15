package com.otp.otp_auth.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "locked_users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_locked_user_user_id", columnNames = "user_id")
})
@Getter
@Setter
public class LockedUsers {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Ensure 1 record per user (unique constraint above)
    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_locked_users_user"))
    private Users user;

    @Column(name = "locked_until", nullable = false)
    private LocalDateTime lockedUntil;
}
