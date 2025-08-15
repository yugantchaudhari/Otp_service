package com.otp.otp_auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OtpAuthApplication {

	public static void main(String[] args) {
		SpringApplication.run(OtpAuthApplication.class, args);
	}

}
