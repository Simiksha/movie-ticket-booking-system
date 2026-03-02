package com.moviebooking.movie_ticket_booking.repository;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.moviebooking.movie_ticket_booking.model.Genre;
import com.moviebooking.movie_ticket_booking.model.Movie;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
public class MovieRepositoryTest {
    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Genre actionGenre;
    private Genre comedyGenre;

    @BeforeEach
    void setUp() {
        actionGenre = Genre.builder().name("Action").build();
        comedyGenre = Genre.builder().name("Comedy").build();
        entityManager.persist(actionGenre);
        entityManager.persist(comedyGenre);
    }

    @Test
    void shouldFindOnlyActiveMovies() {
        // Given
        Movie activeMovie = createMovie("Inception", true, Set.of(actionGenre));
        Movie inactiveMovie = createMovie("Old Movie", false, Set.of(actionGenre));
        entityManager.persist(activeMovie);
        entityManager.persist(inactiveMovie);

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Movie> result = movieRepository.findByActiveTrue(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Inception");
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void shouldFindByGenreNameAndActiveTrue() {
        // Given
        Movie actionMovie = createMovie("Die Hard", true, Set.of(actionGenre));
        Movie comedyMovie = createMovie("Hangover", true, Set.of(comedyGenre));
        Movie inactiveActionMovie = createMovie("Old Action", false, Set.of(actionGenre));
        
        entityManager.persist(actionMovie);
        entityManager.persist(comedyMovie);
        entityManager.persist(inactiveActionMovie);

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Movie> result = movieRepository.findByGenres_NameAndActiveTrue("Action", pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Die Hard");
        assertThat(result.getContent().get(0).getGenres()).contains(actionGenre);
    }

    // Helper method to reduce boilerplate
    private Movie createMovie(String title, boolean active, Set<Genre> genres) {
        return Movie.builder()
                .title(title)
                .description("Sample Description")
                .duration(120)
                .language("English")
                .rating("PG-13")
                .releaseDate(LocalDate.now())
                .active(active)
                .genres(genres)
                .build();
    }
}
