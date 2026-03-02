package com.moviebooking.movie_ticket_booking.theater;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class TheaterRepositoryTest {
    @Autowired
    private TheaterRepository theaterRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldFindTheaterByNameAndCity() {
        // Given
        Theater theater = Theater.builder()
                .name("PVR")
                .city("Chennai")
                .address("Main Mall")
                .active(true)
                .build();
        entityManager.persistAndFlush(theater);

        // When
        Optional<Theater> found = theaterRepository.findByNameAndCity("PVR", "Chennai");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("PVR");
        assertThat(found.get().getCity()).isEqualTo("Chennai");
    }

    @Test
    void shouldReturnEmptyWhenTheaterDoesNotExistInCity() {
        // Given
        Theater theater = Theater.builder()
                .name("PVR")
                .city("Mumbai")
                .address("Ocean Drive")
                .active(true)
                .build();
        entityManager.persistAndFlush(theater);

        // When: Searching for same name but different city
        Optional<Theater> found = theaterRepository.findByNameAndCity("PVR", "Chennai");

        // Then
        assertThat(found).isEmpty();
    }
}
