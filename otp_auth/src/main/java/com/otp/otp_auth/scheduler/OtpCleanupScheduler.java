package com.otp.otp_auth.scheduler;

import com.otp.otp_auth.repository.LockedUserRepository;
import com.otp.otp_auth.repository.OtpRegistrationsRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class OtpCleanupScheduler {

    @Autowired
    private OtpRegistrationsRepository otpRegistrationsRepository;

    @Autowired
    private LockedUserRepository lockedUserRepository;

    /**
     * Remove expired registration OTPs every 10 minutes.
     */
    @Transactional
    @Scheduled(fixedRate = 600_000)
    public void cleanupExpiredOtpRegistrations() {
        otpRegistrationsRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        System.out.println("Cleaned up expired OTP registration records at " + LocalDateTime.now());
    }
    /**
     * Remove expired lockout records every 5 minutes.
     */
    @Transactional
    // @Scheduled(fixedRate = 300_000) // 5 minutes
    @Scheduled(fixedRate = 120_000) 
    public void cleanupExpiredLockouts() {
        int deleted = lockedUserRepository.deleteByLockedUntilBefore(LocalDateTime.now());
        System.out.println("Cleaned up " + deleted + " expired lockout records at " + LocalDateTime.now());
    }
}
