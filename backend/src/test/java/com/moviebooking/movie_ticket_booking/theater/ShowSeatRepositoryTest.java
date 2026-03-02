package com.moviebooking.movie_ticket_booking.theater;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.moviebooking.movie_ticket_booking.model.Movie;
import com.moviebooking.movie_ticket_booking.model.Show;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


@DataJpaTest
public class ShowSeatRepositoryTest {
    @Autowired
    private ShowSeatRepository showSeatRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Show savedShow;
    private Seat savedSeat;

    @BeforeEach
    void setUp() {
        // 1. Setup Theater & Screen
        Theater theater = Theater.builder().name("PVR").city("Delhi").address("address").build();
        entityManager.persist(theater);

        Screen screen = Screen.builder().name("Audi 1").totalSeats(50).theater(theater).build();
        entityManager.persist(screen);

        // 2. Setup Seat (Belongs to Screen)
        savedSeat = Seat.builder()
                .screen(screen)
                .seatNumber("A1")
                .seatType(SeatType.PREMIUM) 
                .build();
        entityManager.persist(savedSeat);

        // 3. Setup Movie & Show
        Movie movie = Movie.builder()
                .title("The Dark Knight")
                .duration(152).language("English").rating("UA")
                .releaseDate(LocalDate.now())
                .build();
        entityManager.persist(movie);

        savedShow = Show.builder()
                .movie(movie)
                .screen(screen)
                .showTime(LocalDateTime.now())
                .price(BigDecimal.valueOf(250))
                .build();
        entityManager.persist(savedShow);

        entityManager.flush();
    }

    @Test
    void shouldFindByShowIdAndSeatId() {
        // Given
        ShowSeat showSeat = ShowSeat.builder()
                .show(savedShow)
                .seat(savedSeat)
                .booked(false)
                .price(BigDecimal.valueOf(300))
                .build();
        entityManager.persistAndFlush(showSeat);

        // When
        Optional<ShowSeat> found = showSeatRepository.findByShowIdAndSeatId(savedShow.getId(), savedSeat.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getPrice()).isEqualTo(BigDecimal.valueOf(300));
    }

    @Test
    void shouldFindByIdForUpdate() {
        // Given
        ShowSeat showSeat = ShowSeat.builder()
                .show(savedShow)
                .seat(savedSeat)
                .booked(false)
                .price(BigDecimal.valueOf(300))
                .build();
        ShowSeat saved = entityManager.persistAndFlush(showSeat);

        // When - Triggers PESSIMISTIC_WRITE lock
        ShowSeat locked = showSeatRepository.findByIdForUpdate(saved.getId());

        // Then
        assertThat(locked).isNotNull();
        assertThat(locked.getId()).isEqualTo(saved.getId());
    }

    @Test
    void shouldDeleteByShowId() {
        // Given
        ShowSeat showSeat = ShowSeat.builder()
                .show(savedShow)
                .seat(savedSeat)
                .booked(false)
                .price(BigDecimal.valueOf(300))
                .build();
        entityManager.persistAndFlush(showSeat);

        // When
        showSeatRepository.deleteByShowId(savedShow.getId());
        entityManager.flush();
        entityManager.clear();

        // Then
        List<ShowSeat> results = showSeatRepository.findByShowId(savedShow.getId());
        assertThat(results).isEmpty();
    }
}
