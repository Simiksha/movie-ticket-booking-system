package com.moviebooking.movie_ticket_booking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.moviebooking.movie_ticket_booking.config.RazorpayProperties;
import com.moviebooking.movie_ticket_booking.model.Booking;
import com.moviebooking.movie_ticket_booking.model.BookingStatus;
import com.moviebooking.movie_ticket_booking.model.Payment;
import com.moviebooking.movie_ticket_booking.model.PaymentStatus;
import com.moviebooking.movie_ticket_booking.repository.BookingRepository;
import com.moviebooking.movie_ticket_booking.repository.PaymentRepository;
import com.razorpay.Utils;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceWebhookTest {
    
    @Mock private PaymentRepository paymentRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private RazorpayProperties razorpayProperties;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void handleWebhook_paymentCaptured_shouldMarkPaymentSuccess_andConfirmBooking() throws Exception {

        when(razorpayProperties.getWebhookSecret()).thenReturn("whsec_test");

        Booking booking = Booking.builder()
                .id(10L)
                .status(BookingStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        Payment payment = Payment.builder()
                .id(1L)
                .booking(booking)
                .amount(BigDecimal.valueOf(250))
                .status(PaymentStatus.INITIATED)
                .gatewayOrderId("order_123")
                .createdAt(LocalDateTime.now())
                .build();

        when(paymentRepository.findByGatewayOrderId("order_123")).thenReturn(Optional.of(payment));

        String payload = """
                {
                  "event": "payment.captured",
                  "payload": {
                    "payment": {
                      "entity": {
                        "order_id": "order_123",
                        "id": "pay_999"
                      }
                    }
                  }
                }
                """;

        String signature = "dummy_signature";

        try (MockedStatic<Utils> utilsMock = Mockito.mockStatic(Utils.class)) {
            utilsMock.when(() -> Utils.verifyWebhookSignature(payload, signature, "whsec_test"))
                    .thenReturn(true);

            paymentService.handleWebhook(payload, signature);
        }

        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals("pay_999", payment.getGatewayPaymentId());
        assertNotNull(payment.getUpdatedAt());

        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());

        verify(paymentRepository, times(1)).save(payment);
        verify(bookingRepository, times(1)).save(booking);
    }

    @Test
    void handleWebhook_paymentFailed_shouldMarkPaymentFailed_andCancelBooking() throws Exception {

        when(razorpayProperties.getWebhookSecret()).thenReturn("whsec_test");

        Booking booking = Booking.builder()
                .id(10L)
                .status(BookingStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        Payment payment = Payment.builder()
                .id(1L)
                .booking(booking)
                .amount(BigDecimal.valueOf(250))
                .status(PaymentStatus.INITIATED)
                .gatewayOrderId("order_123")
                .createdAt(LocalDateTime.now())
                .build();

        when(paymentRepository.findByGatewayOrderId("order_123")).thenReturn(Optional.of(payment));

        String payload = """
                {
                  "event": "payment.failed",
                  "payload": {
                    "payment": {
                      "entity": {
                        "order_id": "order_123"
                      }
                    }
                  }
                }
                """;

        String signature = "dummy_signature";

        try (MockedStatic<Utils> utilsMock = Mockito.mockStatic(Utils.class)) {
            utilsMock.when(() -> Utils.verifyWebhookSignature(payload, signature, "whsec_test"))
                    .thenReturn(true);

            paymentService.handleWebhook(payload, signature);
        }

        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        assertNotNull(payment.getUpdatedAt());

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());

        verify(paymentRepository, times(1)).save(payment);
        verify(bookingRepository, times(1)).save(booking);
    }

    @Test
    void handleWebhook_invalidSignature_shouldThrow_andNotUpdateAnything() throws Exception {

        when(razorpayProperties.getWebhookSecret()).thenReturn("whsec_test");

        String payload = """
                { "event": "payment.captured", "payload": { "payment": { "entity": { "order_id": "order_123", "id": "pay_999" } } } }
                """;

        String signature = "bad_sig";

        try (MockedStatic<Utils> utilsMock = Mockito.mockStatic(Utils.class)) {
            utilsMock.when(() -> Utils.verifyWebhookSignature(payload, signature, "whsec_test"))
                    .thenReturn(false);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> paymentService.handleWebhook(payload, signature));

            assertTrue(ex.getMessage().contains("Invalid webhook signature"));
        }

        verifyNoInteractions(paymentRepository);
        verifyNoInteractions(bookingRepository);
    }
}
