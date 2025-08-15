package com.otp.otp_auth.repository;

import com.otp.otp_auth.model.LockedUsers;
import com.otp.otp_auth.model.Users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface LockedUserRepository extends JpaRepository<LockedUsers, Long> {
    Optional<LockedUsers> findByUser(Users user);
    /**
     * Delete all locked user records where lock has expired
     * Returns the number of records deleted
     */
    int deleteByLockedUntilBefore(LocalDateTime time);
}
