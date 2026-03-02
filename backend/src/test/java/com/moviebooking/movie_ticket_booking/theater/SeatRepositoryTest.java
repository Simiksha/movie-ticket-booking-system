package com.moviebooking.movie_ticket_booking.theater;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
public class SeatRepositoryTest {
    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Screen savedScreen;

    @BeforeEach
    void setUp() {
        // 1. Create and persist Theater (Parent of Screen)
        Theater theater = Theater.builder()
                .name("Grand Cinema")
                .city("Chennai")
                .address("Anna Salai")
                .active(true)
                .build();
        entityManager.persist(theater);

        // 2. Create and persist Screen (Parent of Seat)
        savedScreen = Screen.builder()
                .name("Audi 1")
                .totalSeats(100)
                .theater(theater)
                .build();
        entityManager.persist(savedScreen);

        entityManager.flush();
    }

    @Test
    void shouldFindByScreenId() {
        // Given: Create two seats for the saved screen
        Seat seat1 = Seat.builder()
                .screen(savedScreen)
                .seatNumber("A1")
                .seatType(SeatType.PREMIUM)
                .build();
        
        Seat seat2 = Seat.builder()
                .screen(savedScreen)
                .seatNumber("A2")
                .seatType(SeatType.PREMIUM)
                .build();

        entityManager.persist(seat1);
        entityManager.persist(seat2);
        entityManager.persistAndFlush(seat2);

        // When
        List<Seat> foundSeats = seatRepository.findByScreenId(savedScreen.getId());

        // Then
        assertThat(foundSeats).hasSize(2);
        assertThat(foundSeats).extracting(Seat::getSeatNumber).containsExactlyInAnyOrder("A1", "A2");
    }

    @Test
    void shouldReturnEmptyWhenNoSeatsExistForScreen() {
        // When: Searching for an ID with no seats assigned
        List<Seat> foundSeats = seatRepository.findByScreenId(999L);

        // Then
        assertThat(foundSeats).isEmpty();
    }
}
