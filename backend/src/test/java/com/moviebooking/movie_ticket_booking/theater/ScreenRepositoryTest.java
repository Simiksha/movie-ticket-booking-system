package com.moviebooking.movie_ticket_booking.theater;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;


@DataJpaTest
public class ScreenRepositoryTest {
    @Autowired
    private ScreenRepository screenRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldFindScreenByNameAndTheaterId() {
        // 1. Create and persist a Theater first
        Theater theater = Theater.builder()
                .name("PVR Cinemas")
                .city("Bangalore")
                .address("Forum Mall")
                .active(true)
                .build();
        Theater savedTheater = entityManager.persistAndFlush(theater);

        // 2. Create and persist a Screen for that Theater
        Screen screen = Screen.builder()
                .name("Audi 1")
                .totalSeats(150)
                .theater(savedTheater)
                .build();
        entityManager.persistAndFlush(screen);

        // 3. When
        Optional<Screen> found = screenRepository.findByNameAndTheaterId("Audi 1", savedTheater.getId());

        // 4. Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Audi 1");
        assertThat(found.get().getTheater().getId()).isEqualTo(savedTheater.getId());
    }

    @Test
    void shouldReturnEmptyWhenScreenNameDoesNotExistInTheater() {
        // Given
        Theater theater = Theater.builder()
                .name("Inox")
                .city("Chennai")
                .address("City Center")
                .build();
        Theater savedTheater = entityManager.persistAndFlush(theater);

        // When: Searching for a name that wasn't saved
        Optional<Screen> found = screenRepository.findByNameAndTheaterId("IMAX", savedTheater.getId());

        // Then
        assertThat(found).isEmpty();
    }
}
