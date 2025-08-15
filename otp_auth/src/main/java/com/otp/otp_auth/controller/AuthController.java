package com.otp.otp_auth.controller;

import com.otp.otp_auth.dto.OtpVerificationRequest;
import com.otp.otp_auth.dto.RegistrationRequest;
import com.otp.otp_auth.model.Users;
import com.otp.otp_auth.repository.UserRepository;
import com.otp.otp_auth.service.OtpService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api")
public class AuthController {

    @Autowired
    private OtpService otpService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Handles the initial user registration request.
     * This endpoint generates an OTP and sends it to the user's email.
     *
     * @param request The RegistrationRequest DTO containing email and password.
     * @return A ResponseEntity with a status and message.
     */
    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@Valid @RequestBody RegistrationRequest request) {
        String email = request.getEmail();
        String password = request.getPassword();

        // 1. Check if user already exists in the permanent table
        Optional<Users> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            return new ResponseEntity<>("Email is already registered.", HttpStatus.CONFLICT);
        }

        // 2. Hash the password before sending it to the service
        String passwordHash = passwordEncoder.encode(password);

        // 3. Generate and send the OTP for registration
        String message = otpService.generateRegistrationOtp(email, passwordHash);
        return new ResponseEntity<>(message, HttpStatus.OK);
    }

    /**
     * Handles the OTP verification for a registration attempt.
     * On success, a new user is created in the database.
     *
     * @param request The OtpVerificationRequest DTO.
     * @return A ResponseEntity with a status and message.
     */
    @PostMapping("/register/verify")
    public ResponseEntity<String> verifyRegistrationOtp(@Valid @RequestBody OtpVerificationRequest request) {
        String message = otpService.verifyRegistrationOtp(request.getEmail(), request.getOtpCode());

        // We assume "Registration successful." indicates success
        if (message.equals("Registration successful.")) {
            // In a real-world app, you might issue a JWT token here.
            return new ResponseEntity<>(message, HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>(message, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Handles the user login request.
     * This endpoint verifies credentials and sends an OTP for 2-factor authentication.
     *
     * @param request The RegistrationRequest DTO containing email and password.
     * @return A ResponseEntity with a status and message.
     */
    @PostMapping("/login")
    public ResponseEntity<String> loginUser(@Valid @RequestBody RegistrationRequest request) {
        // 1. Find the user by email
        Optional<Users> userOptional = userRepository.findByEmail(request.getEmail());
        if (userOptional.isEmpty()) {
            return new ResponseEntity<>("Invalid email or password.", HttpStatus.UNAUTHORIZED);
        }

        Users user = userOptional.get();

        // 2. Verify the password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            return new ResponseEntity<>("Invalid email or password.", HttpStatus.UNAUTHORIZED);
        }

        // 3. Generate and send the OTP for login
        String message = otpService.generateLoginOtp(user);
        return new ResponseEntity<>(message, HttpStatus.OK);
    }

    /**
     * Handles the OTP verification for a login attempt.
     * On success, the user is authenticated.
     *
     * @param request The OtpVerificationRequest DTO.
     * @return A ResponseEntity with a status and message.
     */
    @PostMapping("/login/verify")
    public ResponseEntity<String> verifyLoginOtp(@Valid @RequestBody OtpVerificationRequest request) {
        Optional<Users> userOptional = userRepository.findByEmail(request.getEmail());
        if (userOptional.isEmpty()) {
            return new ResponseEntity<>("Invalid OTP or no pending login.", HttpStatus.BAD_REQUEST);
        }
        Users user = userOptional.get();

        String message = otpService.verifyLoginOtp(user, request.getOtpCode());

        // We assume "Login successful." indicates success
        if (message.equals("Login successful.")) {
            // In a real-world app, you would issue a JWT token here.
            return new ResponseEntity<>(message, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(message, HttpStatus.BAD_REQUEST);
        }
    }
}