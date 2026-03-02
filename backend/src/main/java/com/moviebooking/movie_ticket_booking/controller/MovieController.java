package com.moviebooking.movie_ticket_booking.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moviebooking.movie_ticket_booking.dto.CreateMovieRequest;
import com.moviebooking.movie_ticket_booking.dto.MovieResponse;
import com.moviebooking.movie_ticket_booking.dto.UpdateMovieRequest;
import com.moviebooking.movie_ticket_booking.service.MovieService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    // PUBLIC APIs

    @GetMapping("/movies")
    public Page<MovieResponse> getMovies(
            @RequestParam(required = false) String genre,
            Pageable pageable) {

        if (genre != null) {
            return movieService.getMoviesByGenre(genre, pageable);
        }

        return movieService.getAllMovies(pageable);
    }

    // ADMIN APIs

    @PostMapping("/admin/movies")
    public MovieResponse createMovie(
            @Valid @RequestBody CreateMovieRequest request) {

        return movieService.createMovie(request);
    }

    @PutMapping("/admin/movies/{id}")
    public MovieResponse updateMovie(
            @PathVariable Long id,
            @RequestBody UpdateMovieRequest request) {

        return movieService.updateMovie(id, request);
    }

    @DeleteMapping("/admin/movies/{id}")
    public void deleteMovie(@PathVariable Long id) {

        movieService.deleteMovie(id);
    }
}
