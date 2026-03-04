package com.moviebooking.movie_ticket_booking.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moviebooking.movie_ticket_booking.dto.BookingHistoryResponse;
import com.moviebooking.movie_ticket_booking.dto.BookingRequest;
import com.moviebooking.movie_ticket_booking.exception.BadRequestException;
import com.moviebooking.movie_ticket_booking.model.Booking;
import com.moviebooking.movie_ticket_booking.model.BookingSeat;
import com.moviebooking.movie_ticket_booking.model.BookingStatus;
import com.moviebooking.movie_ticket_booking.model.Movie;
import com.moviebooking.movie_ticket_booking.model.Payment;
import com.moviebooking.movie_ticket_booking.model.Show;
import com.moviebooking.movie_ticket_booking.model.User;
import com.moviebooking.movie_ticket_booking.repository.BookingRepository;
import com.moviebooking.movie_ticket_booking.repository.BookingSeatRepository;
import com.moviebooking.movie_ticket_booking.repository.PaymentRepository;
import com.moviebooking.movie_ticket_booking.repository.ShowRepository;
import com.moviebooking.movie_ticket_booking.repository.UserRepository;
import com.moviebooking.movie_ticket_booking.theater.ShowSeat;
import com.moviebooking.movie_ticket_booking.theater.ShowSeatRepository;
import com.moviebooking.movie_ticket_booking.theater.Theater;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class BookingService {
        private final BookingRepository bookingRepository;
        private final BookingSeatRepository bookingSeatRepository;
        private final UserRepository userRepository;
        private final ShowSeatRepository showSeatRepository;
        private final PaymentRepository paymentRepository;
        private final EmailService emailService;
        private final ShowRepository showRepository;
        private static final Logger log = LoggerFactory.getLogger(BookingService.class);

        @Transactional
        public Long createBooking(String userEmail, BookingRequest request) {

                User user = userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                Long showId = request.getShowId();
                List<Long> seatIds = request.getSeatIds();

                if (seatIds == null || seatIds.isEmpty()) {
                        throw new RuntimeException("No seats selected");
                }

                // prevent booking if show already started
                Show show = showRepository.findById(showId)
                                .orElseThrow(() -> new RuntimeException("Show not found"));

                if (!show.getShowTime().isAfter(LocalDateTime.now())) {
                        throw new BadRequestException("Show already started. Booking not allowed.");
                }

                List<ShowSeat> showSeats = showSeatRepository
                                .findByShowIdAndSeatIdInForUpdate(showId, seatIds);

                if (showSeats.size() != seatIds.size()) {
                        throw new RuntimeException("One or more seats not found for this show");
                }

                for (ShowSeat ss : showSeats) {
                        if (ss.isBooked()) {
                                throw new RuntimeException("Seat already booked: " + ss.getSeat().getSeatNumber());
                        }
                }

                Booking booking = Booking.builder()
                                .user(user)
                                .show(show)
                                .status(BookingStatus.PENDING)
                                .totalAmount(BigDecimal.ZERO)
                                .createdAt(LocalDateTime.now())
                                .expiresAt(LocalDateTime.now().plusMinutes(10))
                                .build();

                bookingRepository.save(booking);

                BigDecimal totalAmount = BigDecimal.ZERO;

                for (ShowSeat ss : showSeats) {
                        ss.setBooked(true);

                        bookingSeatRepository.save(BookingSeat.builder()
                                        .booking(booking)
                                        .showSeat(ss)
                                        .build());

                        totalAmount = totalAmount.add(ss.getPrice());
                }

                booking.setTotalAmount(totalAmount);
                bookingRepository.save(booking);
                return booking.getId();
        }

        private String buildBookingEmailContent(Booking booking) {

                return """
                                🎬 Booking Confirmed!

                                Movie: %s
                                Theater: %s
                                Show Time: %s
                                Seats: %s

                                Enjoy your show!
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

        @Transactional
        public void confirmBooking(Long bookingId) {
                log.info("CONFIRM_BOOKING called bookingId={}", bookingId);

                Booking booking = bookingRepository.findById(bookingId)
                                .orElseThrow(() -> new RuntimeException("Booking not found"));

                if (booking.getExpiresAt().isBefore(LocalDateTime.now())) {
                        if (booking.getStatus() == BookingStatus.PENDING) {
                                booking.setStatus(BookingStatus.EXPIRED);
                                bookingRepository.save(booking);
                        }
                        throw new RuntimeException("Booking has expired");
                }

                // Only pending or confirmed are valid states here
                if (booking.getStatus() == BookingStatus.PENDING) {
                        booking.setStatus(BookingStatus.CONFIRMED);
                        bookingRepository.save(booking);
                } else if (booking.getStatus() != BookingStatus.CONFIRMED) {
                        throw new RuntimeException("Booking is not pending. Current status: " + booking.getStatus());
                }

                // Try to flip false->true once; only one request will succeed
                int updated = bookingRepository.markConfirmationEmailSent(bookingId);
                log.info("markConfirmationEmailSent updatedRows={} bookingId={}", updated, bookingId);

                if (updated == 1) {
                        emailService.sendBookingConfirmation(
                                        booking.getUser().getEmail(),
                                        "Booking Confirmed 🎬",
                                        buildBookingEmailContent(booking));
                }
        }

        @Scheduled(fixedRate = 60000) // runs every 60 seconds
        @Transactional
        public void expirePendingBookings() {
                // Find all pending bookings that have passed their expiry time
                List<Booking> expiredBookings = bookingRepository
                                .findAllByStatusAndExpiresAtBefore(BookingStatus.PENDING, LocalDateTime.now());

                for (Booking booking : expiredBookings) {
                        // Mark booking as expired
                        booking.setStatus(BookingStatus.EXPIRED);

                        // Release seats by marking ShowSeat as not booked
                        booking.getBookingSeats().forEach(bs -> {
                                ShowSeat showSeat = bs.getShowSeat();
                                showSeat.setBooked(false);
                        });
                }

                // Save the updated bookings and seats
                bookingRepository.saveAll(expiredBookings);

        }

        @Transactional(readOnly = true)
        public List<BookingHistoryResponse> getMyBookings(String email) {

                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                List<Booking> bookings = bookingRepository.findByUserOrderByCreatedAtDesc(user);

                return bookings.stream()
                                .map(this::convertToHistoryResponse)
                                .toList();
        }

        private BookingHistoryResponse convertToHistoryResponse(Booking booking) {

                Show show = booking.getShow();
                Movie movie = show.getMovie();
                Theater theater = show.getScreen().getTheater();

                List<String> seatNumbers = booking.getBookingSeats()
                                .stream()
                                .map(bs -> bs.getShowSeat().getSeat().getSeatNumber())
                                .toList();

                Payment payment = paymentRepository
                                .findByBooking(booking)
                                .orElse(null);

                return BookingHistoryResponse.builder()
                                .bookingId(booking.getId())
                                .movieTitle(movie.getTitle())
                                .theaterName(theater.getName())
                                .screenName(booking.getShow().getScreen().getName())
                                .showTime(show.getShowTime())
                                .seats(seatNumbers)
                                .bookingStatus(booking.getStatus().name())
                                .paymentStatus(payment != null ? payment.getStatus().name() : "NOT_PAID")
                                .totalAmount(booking.getTotalAmount())
                                .bookedAt(booking.getCreatedAt())
                                .build();
        }

}
