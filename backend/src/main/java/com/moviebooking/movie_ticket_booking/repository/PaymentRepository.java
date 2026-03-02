package com.moviebooking.movie_ticket_booking.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moviebooking.movie_ticket_booking.model.Booking;
import com.moviebooking.movie_ticket_booking.model.Payment;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    boolean existsByBooking(Booking booking);

    Optional<Payment> findByGatewayOrderId(String gatewayOrderId);

    Optional<Payment> findByBooking(Booking booking);

}
