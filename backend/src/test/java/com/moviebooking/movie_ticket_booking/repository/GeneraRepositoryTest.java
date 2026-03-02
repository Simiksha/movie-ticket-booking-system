package com.moviebooking.movie_ticket_booking.repository;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.moviebooking.movie_ticket_booking.model.Genre;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
public class GeneraRepositoryTest {
    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Should find a genre by its exact name")
    void shouldFindGenreByName() {
        Genre action = Genre.builder().name("Action").build();
        entityManager.persistAndFlush(action); 

        Optional<Genre> found = genreRepository.findByName("Action");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Action");
    }

    @Test
    @DisplayName("Should return empty when searching for a non-existent genre")
    void shouldReturnEmptyForNonExistentName() {

        Optional<Genre> found = genreRepository.findByName("Sci-Fi");

        assertThat(found).isEmpty();
    }
}
