package com.moviebooking.movie_ticket_booking.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moviebooking.movie_ticket_booking.model.Movie;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {  
    
    Page<Movie> findByActiveTrue(Pageable pageable);

    Page<Movie> findByGenres_NameAndActiveTrue(String genre, Pageable pageable);
}
