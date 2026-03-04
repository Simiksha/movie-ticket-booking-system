package com.moviebooking.movie_ticket_booking.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.moviebooking.movie_ticket_booking.model.Booking;
import com.moviebooking.movie_ticket_booking.model.BookingStatus;
import com.moviebooking.movie_ticket_booking.model.User;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findAllByStatusAndExpiresAtBefore(BookingStatus status, LocalDateTime time);

    List<Booking> findByUserOrderByCreatedAtDesc(User user);

    @Query("""
                SELECT DISTINCT b FROM Booking b
                LEFT JOIN FETCH b.bookingSeats bs
                LEFT JOIN FETCH bs.showSeat ss
                WHERE b.status = :status AND b.expiresAt < :now
            """)
    List<Booking> findExpiredPendingWithSeats(@Param("status") BookingStatus status,
            @Param("now") LocalDateTime now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(
        value = "UPDATE bookings SET confirmation_email_sent = true WHERE id = :id AND confirmation_email_sent = false",
        nativeQuery = true
    )
    int markConfirmationEmailSent(@Param("id") Long id);
}
