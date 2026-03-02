package com.moviebooking.movie_ticket_booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moviebooking.movie_ticket_booking.model.BookingSeat;

@Repository
public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {
}
