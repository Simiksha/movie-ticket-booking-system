package com.moviebooking.movie_ticket_booking.theater;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface ScreenRepository extends JpaRepository<Screen, Long> {
    
    Optional<Screen> findByNameAndTheaterId(String name, Long theaterId);

    List<Screen> findByTheaterId(Long theaterId);
}
