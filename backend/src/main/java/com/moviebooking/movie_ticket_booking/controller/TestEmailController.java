package com.moviebooking.movie_ticket_booking.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moviebooking.movie_ticket_booking.service.EmailService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class TestEmailController {
    
    private final EmailService emailService;

    @GetMapping("movies/test-email")
    public String testEmail(@RequestParam String to) {
        emailService.sendBookingConfirmation(to, "Brevo Test", "Hello from Render + Brevo!");
        return "sent";
    }
}
