package com.moviebooking.movie_ticket_booking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "razorpay")
@Getter
@Setter
public class RazorpayProperties {
    private String key;
    private String secret;
    private String webhookSecret;
}
