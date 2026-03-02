package com.moviebooking.movie_ticket_booking.theater;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface TheaterRepository extends  JpaRepository<Theater, Long> {
    
    Optional<Theater> findByNameAndCity(String name, String city);
}
