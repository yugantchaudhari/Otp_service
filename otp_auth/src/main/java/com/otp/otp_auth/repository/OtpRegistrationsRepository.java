package com.otp.otp_auth.repository;

import com.otp.otp_auth.model.OtpRegistrations;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpRegistrationsRepository extends JpaRepository<OtpRegistrations, Long> {
    Optional<OtpRegistrations> findByEmail(String email);
    void deleteByExpiresAtBefore(LocalDateTime dateTime);

}