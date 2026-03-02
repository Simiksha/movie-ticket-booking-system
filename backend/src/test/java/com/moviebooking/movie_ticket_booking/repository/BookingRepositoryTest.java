package com.moviebooking.movie_ticket_booking.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.moviebooking.movie_ticket_booking.model.Booking;
import com.moviebooking.movie_ticket_booking.model.BookingStatus;
import com.moviebooking.movie_ticket_booking.model.Movie;
import com.moviebooking.movie_ticket_booking.model.Role;
import com.moviebooking.movie_ticket_booking.model.Show;
import com.moviebooking.movie_ticket_booking.model.User;
import com.moviebooking.movie_ticket_booking.theater.Screen;
import com.moviebooking.movie_ticket_booking.theater.Theater;


import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
public class BookingRepositoryTest {
    
    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldFindAllByStatusAndExpiresAtBefore() {
        Show show = createAndPersistDependencies();
        User user = createAndPersistUser();
        
        LocalDateTime now = LocalDateTime.now();
        Booking expiredBooking = Booking.builder()
                .user(user)
                .show(show)
                .status(BookingStatus.PENDING)
                .expiresAt(now.minusMinutes(5)) 
                .createdAt(now.minusMinutes(20))
                .totalAmount(BigDecimal.valueOf(200))
                .build();
        entityManager.persist(expiredBooking);

        List<Booking> found = bookingRepository
                .findAllByStatusAndExpiresAtBefore(BookingStatus.PENDING, now);

        assertThat(found).hasSize(1);
    }

    @Test
    void shouldFindByUserOrderByCreatedAtDesc() {
        Show show = createAndPersistDependencies();
        User user = createAndPersistUser();
        
        Booking b1 = createBooking(user, show, LocalDateTime.now().minusDays(1));
        Booking b2 = createBooking(user, show, LocalDateTime.now()); // Newer
        
        entityManager.persist(b1);
        entityManager.persist(b2);

        List<Booking> results = bookingRepository.findByUserOrderByCreatedAtDesc(user);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getCreatedAt()).isAfter(results.get(1).getCreatedAt());
    }


    private User createAndPersistUser() {
        User user = User.builder()
                .name("John")
                .email("user" + System.nanoTime() + "@test.com")
                .password("pass")
                .role(Role.USER)
                .build();
        return entityManager.persist(user);
    }

    private Show createAndPersistDependencies() {
        // 1. Movie
        Movie movie = Movie.builder()
                .title("Interstellar")
                .duration(169).language("English").rating("PG-13")
                .releaseDate(LocalDate.now()).active(true)
                .build();
        entityManager.persist(movie);

        // 2. Theater & Screen
        Theater theater = Theater.builder().name("PVR").city("Mumbai").address("address").build();
        entityManager.persist(theater);

        Screen screen = Screen.builder()
                .name("Screen 1").totalSeats(50).theater(theater)
                .build();
        entityManager.persist(screen);

        // 3. Show (The missing link causing your error)
        Show show = Show.builder()
                .movie(movie).screen(screen)
                .showTime(LocalDateTime.now()).price(BigDecimal.valueOf(250))
                .build();
        return entityManager.persist(show);
    }

    private Booking createBooking(User user, Show show, LocalDateTime createdAt) {
        return Booking.builder()
                .user(user).show(show)
                .status(BookingStatus.CONFIRMED)
                .totalAmount(BigDecimal.valueOf(250))
                .createdAt(createdAt)
                .expiresAt(createdAt.plusHours(2))
                .build();
    }
}
