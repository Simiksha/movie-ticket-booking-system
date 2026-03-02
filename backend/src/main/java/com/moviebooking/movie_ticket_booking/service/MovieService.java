package com.moviebooking.movie_ticket_booking.service;



import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moviebooking.movie_ticket_booking.dto.CreateMovieRequest;
import com.moviebooking.movie_ticket_booking.dto.MovieResponse;
import com.moviebooking.movie_ticket_booking.dto.UpdateMovieRequest;

public interface MovieService {
    
    MovieResponse createMovie(CreateMovieRequest request);

    MovieResponse updateMovie(Long id, UpdateMovieRequest request);

    void deleteMovie(Long id);

    Page<MovieResponse> getAllMovies(Pageable pageable);

    Page<MovieResponse> getMoviesByGenre(String genre, Pageable pageable);
}
