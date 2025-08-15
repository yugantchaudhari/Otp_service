package com.otp.otp_auth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
private String fromEmail;

    /**
     * Sends an email with the OTP code to a user.
     *
     * @param toEmail The recipient's email address.
     * @param otpCode The OTP code to be sent.
     */
    public void sendOtpEmail(String toEmail, String otpCode) {
        log.info("ready to send message by otp");
        System.out.println("otp email service");
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail); // Replace with your email
        message.setTo(toEmail);
        message.setSubject("Your OTP for Verification");
        message.setText("Your one-time password (OTP) is: " + otpCode + ". It is valid for 5 minutes.");

        mailSender.send(message);
        log.info("messaneg send");
    }
}