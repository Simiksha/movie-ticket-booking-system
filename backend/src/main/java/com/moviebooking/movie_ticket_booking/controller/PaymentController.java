package com.moviebooking.movie_ticket_booking.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moviebooking.movie_ticket_booking.service.PaymentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-order/{bookingId}")
    public ResponseEntity<?> createOrder(@PathVariable Long bookingId) throws Exception {
        return ResponseEntity.ok(paymentService.createOrder(bookingId));
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) throws Exception {

        paymentService.handleWebhook(payload, signature);

        return ResponseEntity.ok("Webhook processed");
    }

    @PostMapping("/verify/{bookingId}")
    public ResponseEntity<?> verify(@PathVariable Long bookingId,
            @RequestBody Map<String, String> body) throws Exception {
        paymentService.verifyPayment(bookingId, body);
        return ResponseEntity.ok(Map.of("status", "CONFIRMED"));
    }
}
