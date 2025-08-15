package com.otp.otp_auth.service;

import com.otp.otp_auth.model.LockedUsers;
import com.otp.otp_auth.model.OtpDetails;
import com.otp.otp_auth.model.OtpRegistrations;
import com.otp.otp_auth.model.Users;
import com.otp.otp_auth.repository.LockedUserRepository;
import com.otp.otp_auth.repository.OtpDetailsRepository;
import com.otp.otp_auth.repository.OtpRegistrationsRepository;
import com.otp.otp_auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class OtpService {

    @Autowired
    private OtpRegistrationsRepository otpRegistrationsRepository;

    @Autowired
    private OtpDetailsRepository otpDetailsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LockedUserRepository lockedUserRepository;

    @Autowired
    private EmailService emailService;

    // private static final int OTP_EXPIRY_MINUTES = 5;
    // private static final int MAX_FAILED_ATTEMPTS = 3;
    // private static final int LOCKOUT_DURATION_MINUTES = 15;
    private static final int OTP_EXPIRY_MINUTES = 2;
    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final int LOCKOUT_DURATION_MINUTES = 2;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    // -----------------------------------
    // Registration OTP
    // -----------------------------------

    public String generateRegistrationOtp(String email, String passwordHash) {
        // Check if user already exists
        if (userRepository.findByEmail(email).isPresent()) {
            return "User with this email already exists.";
        }

        // Remove any previous pending OTP registration for this email
        otpRegistrationsRepository.findByEmail(email)
                .ifPresent(otpRegistrationsRepository::delete);

        // Generate OTP
        String otpCode = generateRandomOtp();

        // Save registration attempt with encrypted OTP
        OtpRegistrations registration = new OtpRegistrations();
        registration.setEmail(email);
        registration.setPasswordHash(passwordHash);
        registration.setOtpCode(encoder.encode(otpCode)); // store hashed OTP
        registration.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        registration.setFailedAttempts(0);
        otpRegistrationsRepository.save(registration);

        // Send OTP via email (plain OTP, not hashed)
        emailService.sendOtpEmail(email, otpCode);

        return "OTP sent successfully for registration.";
    }

    public String verifyRegistrationOtp(String email, String otpCode) {
        Optional<OtpRegistrations> regOpt = otpRegistrationsRepository.findByEmail(email);

        if (regOpt.isEmpty()) {
            return "No pending registration found for this email.";
        }

        OtpRegistrations registration = regOpt.get();

        // Check expiry
        if (registration.getExpiresAt().isBefore(LocalDateTime.now())) {
            otpRegistrationsRepository.delete(registration);
            return "OTP has expired. Please request a new one.";
        }

        // Match check using hashed OTP
        if (encoder.matches(otpCode, registration.getOtpCode())) {
            Users newUser = new Users();
            newUser.setEmail(registration.getEmail());
            newUser.setPasswordHash(registration.getPasswordHash());
            userRepository.save(newUser);

            otpRegistrationsRepository.delete(registration);
            return "Registration successful.";
        }

        // Handle wrong OTP
        int fails = registration.getFailedAttempts() + 1;
        registration.setFailedAttempts(fails);

        if (fails >= MAX_FAILED_ATTEMPTS) {
            otpRegistrationsRepository.delete(registration);
            return "Maximum failed attempts reached. Please register again.";
        }

        otpRegistrationsRepository.save(registration);
        return "Invalid OTP. " + (MAX_FAILED_ATTEMPTS - fails) + " attempts remaining.";
    }

    // -----------------------------------
    // Login OTP
    // -----------------------------------

    public String generateLoginOtp(Users user) {
        // Check if account is locked
        Optional<LockedUsers> lockedOpt = lockedUserRepository.findByUser(user);
        if (lockedOpt.isPresent()) {
            LockedUsers locked = lockedOpt.get();
            if (locked.getLockedUntil().isAfter(LocalDateTime.now())) {
                return "Account locked until " + locked.getLockedUntil();
            }
            lockedUserRepository.delete(locked); // remove expired lock
        }

        // Remove any existing OTP for this user
        otpDetailsRepository.findByUser(user)
                .ifPresent(otpDetailsRepository::delete);

        // Generate OTP
        String otpCode = generateRandomOtp();

        // Save OTP with encryption
        OtpDetails loginOtp = new OtpDetails();
        loginOtp.setUser(user);
        loginOtp.setOtpCode(encoder.encode(otpCode)); // store hashed OTP
        loginOtp.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        loginOtp.setFailedAttempts(0);
        otpDetailsRepository.save(loginOtp);

        // Send OTP
        emailService.sendOtpEmail(user.getEmail(), otpCode);

        return "OTP sent successfully for login.";
    }

    public String verifyLoginOtp(Users user, String otpCode) {
        // Lock check
        Optional<LockedUsers> lockOpt = lockedUserRepository.findByUser(user);
        if (lockOpt.isPresent() && lockOpt.get().getLockedUntil().isAfter(LocalDateTime.now())) {
            return "Account is locked until " + lockOpt.get().getLockedUntil();
        }

        Optional<OtpDetails> otpOpt = otpDetailsRepository.findByUser(user);
        if (otpOpt.isEmpty()) {
            return "No OTP pending for this user.";
        }

        OtpDetails loginOtp = otpOpt.get();

        // Expiry check
        if (loginOtp.getExpiresAt().isBefore(LocalDateTime.now())) {
            otpDetailsRepository.delete(loginOtp);
            return "OTP has expired. Please request a new one.";
        }

        // Match check using hashed OTP
        if (encoder.matches(otpCode, loginOtp.getOtpCode())) {
            otpDetailsRepository.delete(loginOtp);
            return "Login successful.";
        }

        // Handle wrong OTP
        int fails = loginOtp.getFailedAttempts() + 1;
        loginOtp.setFailedAttempts(fails);

        if (fails >= MAX_FAILED_ATTEMPTS) {
            otpDetailsRepository.delete(loginOtp);

            LockedUsers lock = new LockedUsers();
            lock.setUser(user);
            lock.setLockedUntil(LocalDateTime.now().plusMinutes(LOCKOUT_DURATION_MINUTES));
            lockedUserRepository.save(lock);

            return "Too many failed attempts. Account locked for " + LOCKOUT_DURATION_MINUTES + " minutes.";
        }

        otpDetailsRepository.save(loginOtp);
        return "Invalid OTP. " + (MAX_FAILED_ATTEMPTS - fails) + " attempts remaining.";
    }

    // -----------------------------------
    // Helper method
    // -----------------------------------
    private String generateRandomOtp() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }
}
