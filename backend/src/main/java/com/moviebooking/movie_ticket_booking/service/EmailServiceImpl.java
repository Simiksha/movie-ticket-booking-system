package com.moviebooking.movie_ticket_booking.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    
    private final BrevoEmailService brevoEmailService;

    @Override
    public void sendBookingConfirmation(String toEmail, String subject, String body) {
        brevoEmailService.sendTextEmail(toEmail, subject, body);
        System.out.println("BOOKING EMAIL METHOD CALLED");
    }
    
}
