package com.moviebooking.movie_ticket_booking.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moviebooking.movie_ticket_booking.dto.CreateMovieRequest;
import com.moviebooking.movie_ticket_booking.dto.MovieResponse;
import com.moviebooking.movie_ticket_booking.dto.UpdateMovieRequest;
import com.moviebooking.movie_ticket_booking.exception.ResourceNotFoundException;
import com.moviebooking.movie_ticket_booking.model.Genre;
import com.moviebooking.movie_ticket_booking.model.Movie;
import com.moviebooking.movie_ticket_booking.repository.GenreRepository;
import com.moviebooking.movie_ticket_booking.repository.MovieRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class MovieServiceImpl implements MovieService {
    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;

    // ================= CREATE MOVIES =================

    @Override
    public MovieResponse createMovie(CreateMovieRequest request) {

        Set<Genre> genres = request.getGenres().stream()
                .map(this::getOrCreateGenre)
                .collect(Collectors.toSet());

        Movie movie = Movie.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .genres(genres)
                .duration(request.getDuration())
                .language(request.getLanguage())
                .rating(request.getRating())
                .releaseDate(request.getReleaseDate())
                .posterUrl(request.getPosterUrl())
                .active(true)
                .build();

        movieRepository.save(movie);

        return mapToResponse(movie);
    }

    // ================= UPDATE MOVIES =================

    @Override
    @Transactional
    public MovieResponse updateMovie(Long id, UpdateMovieRequest request) {

        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id " + id));

        if (request.getTitle() != null)
            movie.setTitle(request.getTitle());
        if (request.getDescription() != null)
            movie.setDescription(request.getDescription());

        if (request.getGenres() != null) {
            Set<Genre> genres = request.getGenres().stream()
                    .map(this::getOrCreateGenre)
                    .collect(Collectors.toSet());
            movie.setGenres(genres);
        }

        if (request.getDuration() != null)
            movie.setDuration(request.getDuration());
        if (request.getLanguage() != null)
            movie.setLanguage(request.getLanguage());
        if (request.getRating() != null)
            movie.setRating(request.getRating());
        if (request.getReleaseDate() != null)
            movie.setReleaseDate(request.getReleaseDate());
        if (request.getPosterUrl() != null)
            movie.setPosterUrl(request.getPosterUrl());
        if (request.getActive() != null)
            movie.setActive(request.getActive());

        Movie saved = movieRepository.saveAndFlush(movie);
        return mapToResponse(saved);
    }

    // ================= DELETE MOVIES =================

    @Override
    public void deleteMovie(Long id) {

        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id " + id));

        movie.setActive(false);
    }

    // ================= GET ALL MOVIES =================

    @Override
    public Page<MovieResponse> getAllMovies(Pageable pageable) {

        return movieRepository.findByActiveTrue(pageable)
                .map(this::mapToResponse);
    }

    // ================= FILTER BY GENRE =================

    @Override
    public Page<MovieResponse> getMoviesByGenre(String genre, Pageable pageable) {

        return movieRepository.findByGenres_NameAndActiveTrue(genre.toUpperCase(), pageable)
                .map(this::mapToResponse);
    }

    private Genre getOrCreateGenre(String name) {

        String normalized = name.trim().toUpperCase();

        return genreRepository.findByName(normalized)
                .orElseGet(() -> genreRepository.save(
                        Genre.builder()
                                .name(normalized)
                                .build()));
    }

    private MovieResponse mapToResponse(Movie movie) {

        return MovieResponse.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .description(movie.getDescription())
                .genres(movie.getGenres().stream()
                        .map(Genre::getName)
                        .collect(Collectors.toSet()))
                .duration(movie.getDuration())
                .language(movie.getLanguage())
                .rating(movie.getRating())
                .releaseDate(movie.getReleaseDate())
                .posterUrl(movie.getPosterUrl())
                .build();
    }

    @Override
    public Page<MovieResponse> getAvailableMovies(String genre, LocalDate date, String city, Pageable pageable) {

        String safeCity = (city == null || city.isBlank()) ? null : city.trim();
        String safeGenre = (genre == null || genre.isBlank()) ? null : genre.trim().toUpperCase();

        LocalDateTime start = null;
        LocalDateTime end = null;
        if (date != null) {
            start = date.atStartOfDay();
            end = date.atTime(LocalTime.MAX);
        }

        return movieRepository.findAvailableMovies(safeGenre, safeCity, start, end, pageable)
                .map(this::mapToResponse);
    }
}
