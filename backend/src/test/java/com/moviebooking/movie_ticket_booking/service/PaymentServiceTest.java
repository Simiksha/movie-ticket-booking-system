package com.moviebooking.movie_ticket_booking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
import com.moviebooking.movie_ticket_booking.model.BookingSeat;
import com.moviebooking.movie_ticket_booking.model.BookingStatus;
import com.moviebooking.movie_ticket_booking.model.Movie;
import com.moviebooking.movie_ticket_booking.model.Payment;
import com.moviebooking.movie_ticket_booking.model.PaymentStatus;
import com.moviebooking.movie_ticket_booking.model.Show;
import com.moviebooking.movie_ticket_booking.model.User;
import com.moviebooking.movie_ticket_booking.repository.BookingRepository;
import com.moviebooking.movie_ticket_booking.repository.PaymentRepository;
import com.moviebooking.movie_ticket_booking.theater.Screen;
import com.moviebooking.movie_ticket_booking.theater.Seat;
import com.moviebooking.movie_ticket_booking.theater.SeatType;
import com.moviebooking.movie_ticket_booking.theater.ShowSeat;
import com.moviebooking.movie_ticket_booking.theater.Theater;
import com.razorpay.Utils;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

        @Mock
        private BookingRepository bookingRepository;
        @Mock
        private PaymentRepository paymentRepository;
        @Mock
        private RazorpayProperties razorpayProperties;
        @Mock
        private EmailService emailService;

        @InjectMocks
        private PaymentService paymentService;

        // createOrder()

        @Test
        void createOrder_bookingNotFound_shouldThrow() {
                when(bookingRepository.findById(1L)).thenReturn(Optional.empty());

                RuntimeException ex = assertThrows(RuntimeException.class,
                                () -> paymentService.createOrder(1L));

                assertTrue(ex.getMessage().contains("Booking not found"));
        }

        @Test
        void createOrder_bookingNotPending_shouldThrow() {
                Booking booking = Booking.builder()
                                .id(1L)
                                .status(BookingStatus.CONFIRMED)
                                .expiresAt(LocalDateTime.now().plusMinutes(10))
                                .build();

                when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

                RuntimeException ex = assertThrows(RuntimeException.class,
                                () -> paymentService.createOrder(1L));

                assertTrue(ex.getMessage().contains("not valid for payment"));
        }

        @Test
        void createOrder_bookingExpired_shouldThrow() {
                Booking booking = Booking.builder()
                                .id(1L)
                                .status(BookingStatus.PENDING)
                                .expiresAt(LocalDateTime.now().minusMinutes(1))
                                .build();

                when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

                RuntimeException ex = assertThrows(RuntimeException.class,
                                () -> paymentService.createOrder(1L));

                assertTrue(ex.getMessage().contains("expired"));
        }

        @Test
        void createOrder_paymentAlreadyExists_shouldThrow() {
                Booking booking = Booking.builder()
                                .id(1L)
                                .status(BookingStatus.PENDING)
                                .expiresAt(LocalDateTime.now().plusMinutes(10))
                                .totalAmount(BigDecimal.valueOf(200))
                                .build();

                when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
                when(paymentRepository.existsByBooking(booking)).thenReturn(true);

                RuntimeException ex = assertThrows(RuntimeException.class,
                                () -> paymentService.createOrder(1L));

                assertTrue(ex.getMessage().contains("already initiated"));
        }

        //  handleWebhook() 

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

                verify(paymentRepository).save(payment);
                verify(bookingRepository).save(booking);
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

                verify(paymentRepository).save(payment);
                verify(bookingRepository).save(booking);
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

        @Test
        void handleWebhook_paymentCaptured_paymentNotFound_shouldThrow() throws Exception {
                when(razorpayProperties.getWebhookSecret()).thenReturn("whsec_test");

                String payload = """
                                {
                                  "event": "payment.captured",
                                  "payload": { "payment": { "entity": { "order_id": "order_X", "id": "pay_1" } } }
                                }
                                """;

                try (MockedStatic<Utils> utilsMock = Mockito.mockStatic(Utils.class)) {
                        utilsMock.when(() -> Utils.verifyWebhookSignature(payload, "sig", "whsec_test"))
                                        .thenReturn(true);

                        RuntimeException ex = assertThrows(RuntimeException.class,
                                        () -> paymentService.handleWebhook(payload, "sig"));

                        assertTrue(ex.getMessage().contains("Payment not found"));
                }
        }

        // cancelAndRefund() 


        @Test
        void cancelAndRefund_wrongUser_shouldThrow() {
                Booking booking = Booking.builder()
                                .id(1L)
                                .user(User.builder().email("real@test.com").build())
                                .status(BookingStatus.CONFIRMED)
                                .show(Show.builder().showTime(LocalDateTime.now().plusHours(5)).build())
                                .build();

                when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

                RuntimeException ex = assertThrows(RuntimeException.class,
                                () -> paymentService.cancelAndRefund(1L, "fake@test.com"));

                assertTrue(ex.getMessage().contains("Not allowed"));
        }

        @Test
        void cancelAndRefund_notConfirmed_shouldThrow() {
                Booking booking = Booking.builder()
                                .id(1L)
                                .user(User.builder().email("user@test.com").build())
                                .status(BookingStatus.PENDING)
                                .show(Show.builder().showTime(LocalDateTime.now().plusHours(5)).build())
                                .build();

                when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

                RuntimeException ex = assertThrows(RuntimeException.class,
                                () -> paymentService.cancelAndRefund(1L, "user@test.com"));

                assertTrue(ex.getMessage().contains("Only confirmed bookings"));
        }

        @Test
        void cancelAndRefund_tooLate_shouldThrow() {
                Booking booking = Booking.builder()
                                .id(1L)
                                .user(User.builder().email("user@test.com").build())
                                .status(BookingStatus.CONFIRMED)
                                .show(Show.builder().showTime(LocalDateTime.now().plusMinutes(10)).build())
                                .build();

                when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

                RuntimeException ex = assertThrows(RuntimeException.class,
                                () -> paymentService.cancelAndRefund(1L, "user@test.com"));

                assertTrue(ex.getMessage().contains("Cancellation allowed"));
        }

        @Test
        void cancelAndRefund_paymentNotFound_shouldThrow() {
                Booking booking = Booking.builder()
                                .id(1L)
                                .user(User.builder().email("user@test.com").build())
                                .status(BookingStatus.CONFIRMED)
                                .show(Show.builder().showTime(LocalDateTime.now().plusHours(5)).build())
                                .bookingSeats(List.of())
                                .build();

                when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
                when(paymentRepository.findByBooking(booking)).thenReturn(Optional.empty());

                RuntimeException ex = assertThrows(RuntimeException.class,
                                () -> paymentService.cancelAndRefund(1L, "user@test.com"));

                assertTrue(ex.getMessage().contains("Payment not found"));
        }

        @Test
        void cancelAndRefund_success_shouldReleaseSeats_cancelBooking_andSendEmail() throws Exception {

                String email = "user@test.com";

                Theater theater = Theater.builder().name("PVR").build();
                Screen screen = Screen.builder().name("Screen 1").theater(theater).build();
                Movie movie = Movie.builder().title("Interstellar").build();

                Show show = Show.builder()
                                .showTime(LocalDateTime.now().plusHours(5))
                                .movie(movie)
                                .screen(screen)
                                .build();

                // Seat needed for email content
                Seat seat = Seat.builder()
                                .seatNumber("A1")
                                .seatType(SeatType.REGULAR)
                                .build();

                // ShowSeat must reference Seat
                ShowSeat showSeat = ShowSeat.builder()
                                .booked(true)
                                .seat(seat)
                                .build();

                BookingSeat bs = BookingSeat.builder()
                                .showSeat(showSeat)
                                .build();

                Booking booking = Booking.builder()
                                .id(1L)
                                .user(User.builder().email(email).build())
                                .status(BookingStatus.CONFIRMED)
                                .show(show)
                                .bookingSeats(List.of(bs))
                                .totalAmount(BigDecimal.valueOf(200))
                                .build();

                // payment status FAILED to skip refund client creation
                Payment payment = Payment.builder()
                                .status(PaymentStatus.FAILED)
                                .booking(booking)
                                .build();

                when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
                when(paymentRepository.findByBooking(booking)).thenReturn(Optional.of(payment));

                paymentService.cancelAndRefund(1L, email);

                assertEquals(BookingStatus.CANCELLED, booking.getStatus());
                assertFalse(showSeat.isBooked());

                verify(emailService, times(1))
                                .sendBookingConfirmation(eq(email), contains("Cancelled"), anyString());
        }
}
