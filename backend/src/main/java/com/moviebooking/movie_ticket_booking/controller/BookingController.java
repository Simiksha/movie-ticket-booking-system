package com.moviebooking.movie_ticket_booking.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moviebooking.movie_ticket_booking.dto.BookingHistoryResponse;
import com.moviebooking.movie_ticket_booking.dto.BookingRequest;
import com.moviebooking.movie_ticket_booking.exception.BadRequestException;
import com.moviebooking.movie_ticket_booking.service.BookingService;
import com.moviebooking.movie_ticket_booking.service.PaymentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final PaymentService paymentService;

    @PostMapping 
    public ResponseEntity<Long> createBooking(
            Principal principal,
            @RequestBody BookingRequest request) {

        if (principal == null) {
            throw new BadRequestException("User must be authenticated");
        }

        Long bookingId = bookingService.createBooking(
                principal.getName(), // email
                request);

        return ResponseEntity.ok(bookingId);
    }

    @GetMapping("/my-bookings")
    public List<BookingHistoryResponse> getMyBookings(
            Principal principal) {
        if (principal == null) {
            throw new BadRequestException("User must be authenticated");
        }

        return bookingService.getMyBookings(principal.getName());
    }

    @DeleteMapping("/{bookingId}")
    public ResponseEntity<String> cancelBooking(
            @PathVariable Long bookingId,
            Principal principal) throws Exception {

        if (principal == null) {
            throw new BadRequestException("User must be authenticated");
        }

        paymentService.cancelAndRefund(
                bookingId,
                principal.getName());

        return ResponseEntity.ok("Booking cancelled successfully");
    }

}
