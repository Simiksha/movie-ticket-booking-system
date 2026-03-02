package com.moviebooking.movie_ticket_booking.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moviebooking.movie_ticket_booking.config.RazorpayProperties;
import com.moviebooking.movie_ticket_booking.exception.BadRequestException;
import com.moviebooking.movie_ticket_booking.model.Booking;
import com.moviebooking.movie_ticket_booking.model.BookingStatus;
import com.moviebooking.movie_ticket_booking.model.Payment;
import com.moviebooking.movie_ticket_booking.model.PaymentStatus;
import com.moviebooking.movie_ticket_booking.repository.BookingRepository;
import com.moviebooking.movie_ticket_booking.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Refund;
import com.razorpay.Utils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

        private final BookingRepository bookingRepository;
        private final PaymentRepository paymentRepository;
        private final RazorpayProperties razorpayProperties;
        private final EmailService emailService;
        private final BookingService bookingService;

        public Map<String, Object> createOrder(Long bookingId) throws Exception {

                // 1. Fetch booking
                Booking booking = bookingRepository.findById(bookingId)
                                .orElseThrow(() -> new RuntimeException("Booking not found"));

                // 2. Validate booking status
                if (booking.getStatus() != BookingStatus.PENDING) {
                        throw new RuntimeException("Booking is not valid for payment");
                }

                // 3. Check expiry
                if (booking.getExpiresAt().isBefore(LocalDateTime.now())) {
                        throw new RuntimeException("Booking has expired");
                }

                // 4. Prevent duplicate payment creation
                if (paymentRepository.existsByBooking(booking)) {
                        throw new RuntimeException("Payment already initiated for this booking");
                }

                // 5. Create Razorpay client
                RazorpayClient razorpayClient = new RazorpayClient(
                                razorpayProperties.getKey(),
                                razorpayProperties.getSecret());

                // 6. Create Razorpay Order
                JSONObject options = new JSONObject();
                options.put("amount",
                                booking.getTotalAmount()
                                                .movePointRight(2)
                                                .setScale(0, RoundingMode.HALF_UP)
                                                .intValue()); // convert to paise
                options.put("currency", "INR");
                options.put("receipt", "booking_" + bookingId);

                Order order = razorpayClient.orders.create(options);

                String razorpayOrderId = order.get("id");

                // 7. Save payment record
                Payment payment = Payment.builder()
                                .booking(booking)
                                .amount(booking.getTotalAmount())
                                .status(PaymentStatus.INITIATED)
                                .gatewayOrderId(razorpayOrderId)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();

                paymentRepository.save(payment);

                // 8. Return response to frontend
                Map<String, Object> response = new HashMap<>();
                response.put("orderId", razorpayOrderId);
                response.put("amount", booking.getTotalAmount());
                response.put("currency", "INR");
                response.put("key", razorpayProperties.getKey());

                return response;
        }

        public void handleWebhook(String payload, String razorpaySignature) throws Exception {

                String webhookSecret = razorpayProperties.getWebhookSecret();

                boolean isValid = Utils.verifyWebhookSignature(payload, razorpaySignature, webhookSecret);

                if (!isValid) {
                        throw new RuntimeException("Invalid webhook signature");
                }

                JSONObject json = new JSONObject(payload);
                String event = json.getString("event");

                if ("payment.captured".equals(event)) {

                        JSONObject paymentEntity = json
                                        .getJSONObject("payload")
                                        .getJSONObject("payment")
                                        .getJSONObject("entity");

                        String orderId = paymentEntity.getString("order_id");
                        String paymentId = paymentEntity.getString("id");

                        Payment payment = paymentRepository
                                        .findByGatewayOrderId(orderId)
                                        .orElseThrow(() -> new RuntimeException("Payment not found"));

                        payment.setGatewayPaymentId(paymentId);
                        payment.setStatus(PaymentStatus.SUCCESS);
                        payment.setUpdatedAt(LocalDateTime.now());

                        paymentRepository.save(payment);

                        // Confirm booking

                        Booking booking = payment.getBooking();
                        booking.setStatus(BookingStatus.CONFIRMED);
                        bookingRepository.save(booking);
                }

                if ("payment.failed".equals(event)) {

                        JSONObject paymentEntity = json
                                        .getJSONObject("payload")
                                        .getJSONObject("payment")
                                        .getJSONObject("entity");

                        String orderId = paymentEntity.getString("order_id");

                        Payment payment = paymentRepository
                                        .findByGatewayOrderId(orderId)
                                        .orElseThrow(() -> new RuntimeException("Payment not found"));

                        payment.setStatus(PaymentStatus.FAILED);
                        payment.setUpdatedAt(LocalDateTime.now());

                        paymentRepository.save(payment);

                        Booking booking = payment.getBooking();
                        booking.setStatus(BookingStatus.CANCELLED);
                        bookingRepository.save(booking);
                }
        }

        // ================= PAYMENT VERIFICATION =================

        public void verifyPayment(Long bookingId, Map<String, String> body) throws Exception {
                Booking booking = bookingRepository.findById(bookingId)
                                .orElseThrow(() -> new RuntimeException("Booking not found"));

                Payment payment = paymentRepository.findByBooking(booking)
                                .orElseThrow(() -> new RuntimeException("Payment not initiated"));

                if (payment.getStatus() == PaymentStatus.SUCCESS && booking.getStatus() == BookingStatus.CONFIRMED) {
                        return;
                }

                String razorpayOrderId = body.get("razorpay_order_id");
                String razorpayPaymentId = body.get("razorpay_payment_id");
                String razorpaySignature = body.get("razorpay_signature");

                if (razorpayOrderId == null || razorpayPaymentId == null || razorpaySignature == null) {
                        throw new RuntimeException("Missing Razorpay verification fields");
                }

                if (!razorpayOrderId.equals(payment.getGatewayOrderId())) {
                        throw new RuntimeException("Order ID mismatch");
                }

                boolean ok = Utils.verifyPaymentSignature(
                                new JSONObject()
                                                .put("razorpay_order_id", razorpayOrderId)
                                                .put("razorpay_payment_id", razorpayPaymentId)
                                                .put("razorpay_signature", razorpaySignature),
                                razorpayProperties.getSecret());

                if (!ok)
                        throw new RuntimeException("Invalid payment signature");

                if (payment.getStatus() != PaymentStatus.SUCCESS) {
                        payment.setGatewayPaymentId(razorpayPaymentId);
                        payment.setStatus(PaymentStatus.SUCCESS);
                        payment.setUpdatedAt(LocalDateTime.now());
                        paymentRepository.save(payment);
                }

                bookingService.confirmBooking(bookingId);
        }

        private String buildCancellationEmailContent(Booking booking) {

                return """
                                ❌ Booking Cancelled Successfully

                                Movie: %s
                                Theater: %s
                                Show Time: %s
                                Seats: %s

                                Your refund has been processed successfully.

                                We hope to see you again!
                                """
                                .formatted(
                                                booking.getShow().getMovie().getTitle(),
                                                booking.getShow().getScreen().getTheater().getName(),
                                                booking.getShow().getShowTime(),
                                                booking.getBookingSeats()
                                                                .stream()
                                                                .map(bs -> bs.getShowSeat()
                                                                                .getSeat()
                                                                                .getSeatNumber())
                                                                .toList());
        }

        // ================= CANCEL TICKETS =================

        @Transactional
        public void cancelAndRefund(Long bookingId, String userEmail) throws Exception {

                Booking booking = bookingRepository.findById(bookingId)
                                .orElseThrow(() -> new RuntimeException("Booking not found"));

                if (!booking.getUser().getEmail().equals(userEmail)) {
                        throw new RuntimeException("Not allowed");
                }

                // prevent double cancel
                if (booking.getStatus() == BookingStatus.CANCELLED) {
                        return;
                }

                if (booking.getStatus() != BookingStatus.CONFIRMED) {
                        throw new RuntimeException("Only confirmed bookings can be cancelled");
                }

                // Cancellation time restriction
                LocalDateTime showTime = booking.getShow().getShowTime();
                if (showTime.minusMinutes(30).isBefore(LocalDateTime.now())) {
                        throw new BadRequestException("Cancellation allowed only up to 30 minutes before show time");
                }

                Payment payment = paymentRepository.findByBooking(booking)
                                .orElseThrow(() -> new RuntimeException("Payment not found"));

                // prevent double refund
                if (payment.getStatus() == PaymentStatus.REFUNDED || payment.getRefundId() != null) {
                        // already refunded, continue to cancel + release seats if not done
                        booking.getBookingSeats().forEach(bs -> bs.getShowSeat().setBooked(false));
                        booking.setStatus(BookingStatus.CANCELLED);
                        bookingRepository.save(booking);
                        return;
                }

                if (payment.getStatus() == PaymentStatus.SUCCESS) {
                        RazorpayClient razorpayClient = new RazorpayClient(
                                        razorpayProperties.getKey(),
                                        razorpayProperties.getSecret());

                        JSONObject refundRequest = new JSONObject();
                        refundRequest.put("amount",
                                        (int) (booking.getTotalAmount().multiply(BigDecimal.valueOf(100)).intValue()));

                        Refund refund = razorpayClient.payments
                                        .refund(payment.getGatewayPaymentId(), refundRequest);

                        payment.setRefundId(refund.get("id").toString());
                        payment.setStatus(PaymentStatus.REFUNDED);
                        payment.setUpdatedAt(LocalDateTime.now());
                        paymentRepository.saveAndFlush(payment);
                }

                // Release seats
                booking.getBookingSeats().forEach(bs -> bs.getShowSeat().setBooked(false));

                booking.setStatus(BookingStatus.CANCELLED);

                bookingRepository.saveAndFlush(booking);

                emailService.sendBookingConfirmation(
                                booking.getUser().getEmail(),
                                "Booking Cancelled ❌",
                                buildCancellationEmailContent(booking));
        }

}
