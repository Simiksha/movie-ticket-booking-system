package com.moviebooking.movie_ticket_booking.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.moviebooking.movie_ticket_booking.model.Genre;
import com.moviebooking.movie_ticket_booking.model.Movie;
import com.moviebooking.movie_ticket_booking.model.Show;
import com.moviebooking.movie_ticket_booking.repository.GenreRepository;
import com.moviebooking.movie_ticket_booking.repository.MovieRepository;
import com.moviebooking.movie_ticket_booking.repository.ShowRepository;
import com.moviebooking.movie_ticket_booking.theater.Screen;
import com.moviebooking.movie_ticket_booking.theater.ScreenRepository;
import com.moviebooking.movie_ticket_booking.theater.Seat;
import com.moviebooking.movie_ticket_booking.theater.SeatRepository;
import com.moviebooking.movie_ticket_booking.theater.SeatType;
import com.moviebooking.movie_ticket_booking.theater.Theater;
import com.moviebooking.movie_ticket_booking.theater.TheaterRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

        private final TheaterRepository theatreRepository;
        private final ScreenRepository screenRepository;
        private final SeatRepository seatRepository;
        private final MovieRepository movieRepository;
        private final ShowRepository showRepository;
        private final GenreRepository genreRepository;

        @Override
        public void run(String... args) {

                if (movieRepository.count() > 0) {
                        return;
                }

                // Create Theatre
                Theater theater = Theater.builder()
                                .name("PVR")
                                .city("Delhi")
                                .address("Connaught Place")
                                .build();

                theatreRepository.save(theater);

                // Create Screen
                com.moviebooking.movie_ticket_booking.theater.Screen screen = Screen.builder()
                                .name("Screen 1")
                                .theater(theater)
                                .totalSeats(10)
                                .build();

                screenRepository.save(screen);

                // Create Seats
                for (int i = 1; i <= 10; i++) {
                        Seat seat = Seat.builder()
                                        .screen(screen)
                                        .seatNumber("A" + i)
                                        .seatType(SeatType.REGULAR)
                                        .build();

                        seatRepository.save(seat);
                }

                // Create Movie
                Genre action = genreRepository.save(
                                Genre.builder().name("ACTION").build());

                Genre thriller = genreRepository.save(
                                Genre.builder().name("THRILLER").build());

                Movie movie = Movie.builder()
                                .title("Interstellar")
                                .description("Sci-fi")
                                .duration(169)
                                .language("English")
                                .rating("PG-13")
                                .genres(Set.of(action, thriller))
                                .releaseDate(LocalDate.now())
                                .build();

                movieRepository.save(movie);

                // Create Show
                Show show = Show.builder()
                                .movie(movie)
                                .screen(screen)
                                .showTime(LocalDateTime.now().plusHours(2))
                                .price(BigDecimal.valueOf(200))
                                .build();

                showRepository.save(show);
        }
}
