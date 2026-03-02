package com.moviebooking.movie_ticket_booking.service;

public interface EmailService {
    void sendBookingConfirmation(String toEmail, String subject, String body);
}
