package com.moviebooking.movie_ticket_booking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.moviebooking.movie_ticket_booking.model.Booking;
import com.moviebooking.movie_ticket_booking.model.BookingSeat;
import com.moviebooking.movie_ticket_booking.model.BookingStatus;
import com.moviebooking.movie_ticket_booking.model.Movie;
import com.moviebooking.movie_ticket_booking.model.Show;
import com.moviebooking.movie_ticket_booking.repository.BookingRepository;
import com.moviebooking.movie_ticket_booking.theater.Screen;
import com.moviebooking.movie_ticket_booking.theater.Seat;
import com.moviebooking.movie_ticket_booking.theater.SeatType;
import com.moviebooking.movie_ticket_booking.theater.ShowSeat;
import com.moviebooking.movie_ticket_booking.theater.Theater;

@ExtendWith(MockitoExtension.class)
public class BookingExpirationServiceTest {

        Theater theater = Theater.builder().name("PVR").build();
Screen screen = Screen.builder().name("Screen 1").theater(theater).build();
Movie movie = Movie.builder().title("Interstellar").build();
    
    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private BookingExpirationService bookingExpirationService;

    @Test
    void expirePendingBookings_shouldExpireAndReleaseSeats() {

        Show show = Show.builder()
                .id(1L)
                .showTime(LocalDateTime.now().plusHours(2))
                .build();

        Seat seat = Seat.builder()
                .id(1L)
                .seatNumber("A1")
                .seatType(SeatType.REGULAR)
                .build();

        ShowSeat showSeat = ShowSeat.builder()
                .id(100L)
                .show(show)
                .seat(seat)
                .booked(true)  
                .price(BigDecimal.valueOf(200))
                .build();

        BookingSeat bookingSeat = BookingSeat.builder()
                .id(10L)
                .showSeat(showSeat)
                .build();

        Booking expiredBooking = Booking.builder()
                .id(99L)
                .status(BookingStatus.PENDING)
                .expiresAt(LocalDateTime.now().minusMinutes(1)) 
                .bookingSeats(List.of(bookingSeat))
                .build();

        when(bookingRepository.findAllByStatusAndExpiresAtBefore(
                eq(BookingStatus.PENDING),
                any(LocalDateTime.class)
        )).thenReturn(List.of(expiredBooking));

        // Act
        bookingExpirationService.expirePendingBookings();

        // Assert
        assertEquals(BookingStatus.EXPIRED, expiredBooking.getStatus());
        assertFalse(showSeat.isBooked());

        verify(bookingRepository, times(1))
                .findAllByStatusAndExpiresAtBefore(
                        eq(BookingStatus.PENDING),
                        any(LocalDateTime.class));
    }

    @Test
    void expirePendingBookings_shouldDoNothingIfNoExpiredBookings() {

        when(bookingRepository.findAllByStatusAndExpiresAtBefore(
                eq(BookingStatus.PENDING),
                any(LocalDateTime.class)
        )).thenReturn(List.of());

        bookingExpirationService.expirePendingBookings();

        verify(bookingRepository, times(1))
                .findAllByStatusAndExpiresAtBefore(
                        eq(BookingStatus.PENDING),
                        any(LocalDateTime.class));
    }
}
