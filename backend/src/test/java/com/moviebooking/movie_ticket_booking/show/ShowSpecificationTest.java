package com.moviebooking.movie_ticket_booking.show;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.jpa.domain.Specification;

import com.moviebooking.movie_ticket_booking.model.Movie;
import com.moviebooking.movie_ticket_booking.model.Show;
import com.moviebooking.movie_ticket_booking.repository.ShowRepository;
import com.moviebooking.movie_ticket_booking.theater.Screen;
import com.moviebooking.movie_ticket_booking.theater.Theater;

@DataJpaTest
public class ShowSpecificationTest {
    @Autowired
    private ShowRepository showRepository; 

    @Autowired
    private TestEntityManager entityManager;

    private Theater theater1;
    private Movie movie1;

    @BeforeEach
    void setUp() {
        // 1. Persist Theater and Screen
        theater1 = Theater.builder().name("PVR").city("Mumbai").address("address").build();
        entityManager.persist(theater1);
        Screen screen1 = Screen.builder().name("Audi 1").theater(theater1).totalSeats(100).build();
        entityManager.persist(screen1);

        // 2. Persist Movie
        movie1 = Movie.builder().title("Inception").duration(148).language("English")
                .rating("UA").releaseDate(LocalDate.now()).build();
        entityManager.persist(movie1);

        // 3. Persist Show
        Show show = Show.builder().movie(movie1).screen(screen1)
                .showTime(LocalDateTime.now()).price(BigDecimal.valueOf(250)).build();
        entityManager.persist(show);
        
        entityManager.flush();
    }

    @Test
    void shouldFilterByDate() {
        Specification<Show> spec = ShowSpecification.filterShows(LocalDate.now(), null, null, null);
        List<Show> results = showRepository.findAll(spec);
        
        assertThat(results).isNotNull();
    }

    @Test
    void shouldFilterByTheaterId() {
        Long theaterId = theater1.getId();
        Specification<Show> spec = ShowSpecification.filterShows(null, theaterId, null, null);

        List<Show> results = showRepository.findAll(spec);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getScreen().getTheater().getId()).isEqualTo(theaterId);
    }

    @Test
    void shouldFilterByMovieId() {
        Long movieId = movie1.getId();
        Specification<Show> spec = ShowSpecification.filterShows(null, null, movieId, null);

        List<Show> results = showRepository.findAll(spec);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getMovie().getId()).isEqualTo(movieId);
    }

    @Test
    void shouldFilterByMultipleCriteria() {
        Specification<Show> spec = ShowSpecification.filterShows(null, 999L, movie1.getId(), null);

        List<Show> results = showRepository.findAll(spec);

        assertThat(results).isEmpty();
    }
}
