package com.moviebooking.movie_ticket_booking.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moviebooking.movie_ticket_booking.model.Booking;
import com.moviebooking.movie_ticket_booking.model.BookingStatus;
import com.moviebooking.movie_ticket_booking.repository.BookingRepository;
import com.moviebooking.movie_ticket_booking.theater.ShowSeat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingExpirationService {
    private final BookingRepository bookingRepository;

    @Scheduled(fixedRate = 60000) // every 60 seconds
    @Transactional
    public void expirePendingBookings() {

        List<Booking> expiredBookings =
                bookingRepository.findExpiredPendingWithSeats(
                        BookingStatus.PENDING,
                        LocalDateTime.now()
                );

        if (expiredBookings.isEmpty()) {
            return;
        }

        log.info("Expiring {} pending bookings", expiredBookings.size());

        for (Booking booking : expiredBookings) {

            booking.setStatus(BookingStatus.EXPIRED);

            booking.getBookingSeats().forEach(bs -> {
                ShowSeat showSeat = bs.getShowSeat();
                showSeat.setBooked(false);
            });
            bookingRepository.saveAll(expiredBookings);
        }
    }
}
