package com.otp.otp_auth.repository;

import com.otp.otp_auth.model.OtpDetails;
import com.otp.otp_auth.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface OtpDetailsRepository extends JpaRepository<OtpDetails, Long> {
    Optional<OtpDetails> findByUser(Users user);
}