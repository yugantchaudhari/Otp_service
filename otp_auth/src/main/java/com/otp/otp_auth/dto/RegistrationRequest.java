package com.otp.otp_auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistrationRequest {

    @NotBlank(message = "Email cannot be empty.")
    @Email(message = "Email should be a valid email address.")
    private String email;

    @NotBlank(message = "Password cannot be empty.")
    private String password;
}