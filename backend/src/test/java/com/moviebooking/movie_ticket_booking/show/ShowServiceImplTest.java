package com.moviebooking.movie_ticket_booking.show;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.moviebooking.movie_ticket_booking.dto.CreateShowRequest;
import com.moviebooking.movie_ticket_booking.dto.ShowResponse;
import com.moviebooking.movie_ticket_booking.dto.ShowSeatResponse;
import com.moviebooking.movie_ticket_booking.dto.UpdateShowRequest;
import com.moviebooking.movie_ticket_booking.exception.ResourceNotFoundException;
import com.moviebooking.movie_ticket_booking.model.Movie;
import com.moviebooking.movie_ticket_booking.model.Show;
import com.moviebooking.movie_ticket_booking.repository.MovieRepository;
import com.moviebooking.movie_ticket_booking.repository.ShowRepository;
import com.moviebooking.movie_ticket_booking.theater.Screen;
import com.moviebooking.movie_ticket_booking.theater.ScreenRepository;
import com.moviebooking.movie_ticket_booking.theater.Seat;
import com.moviebooking.movie_ticket_booking.theater.SeatRepository;
import com.moviebooking.movie_ticket_booking.theater.SeatType;
import com.moviebooking.movie_ticket_booking.theater.ShowSeat;
import com.moviebooking.movie_ticket_booking.theater.ShowSeatRepository;
import com.moviebooking.movie_ticket_booking.theater.Theater;



@ExtendWith(MockitoExtension.class)
public class ShowServiceImplTest {
    @Mock private ShowRepository showRepository;
    @Mock private ShowSeatRepository showSeatRepository;
    @Mock private SeatRepository seatRepository;
    @Mock private ScreenRepository screenRepository;
    @Mock private MovieRepository movieRepository;

    @InjectMocks
    private ShowServiceImpl showService;

    private Movie mockMovie;
    private Screen mockScreen;
    private Theater mockTheater;
    private Show mockShow;

    @BeforeEach
    void setUp() {
        mockMovie = Movie.builder().id(1L).title("Inception").build();
        mockTheater = Theater.builder().id(1L).name("PVR").build();
        mockScreen = Screen.builder().id(1L).name("Audi 1").theater(mockTheater).build();
        mockShow = Show.builder()
                .id(1L)
                .movie(mockMovie)
                .screen(mockScreen)
                .showTime(LocalDateTime.now())
                .price(BigDecimal.valueOf(250))
                .build();
    }

    @Test
    @DisplayName("Should create a show and auto-generate seats")
    void createShow_Success() {
        CreateShowRequest request = new CreateShowRequest(1L, 1L, LocalDateTime.now(), 250.0);
        Seat seat1 = Seat.builder().id(1L).seatNumber("A1").seatType(SeatType.PREMIUM).build();
        
        when(movieRepository.findById(1L)).thenReturn(Optional.of(mockMovie));
        when(screenRepository.findById(1L)).thenReturn(Optional.of(mockScreen));
        when(showRepository.save(any(Show.class))).thenReturn(mockShow);
        when(seatRepository.findByScreenId(1L)).thenReturn(List.of(seat1));

        Long showId = showService.createShow(request);

        assertThat(showId).isEqualTo(1L);
        verify(showRepository).save(any(Show.class));
        verify(showSeatRepository).saveAll(anyList()); 
    }

    @Test
    @DisplayName("Should update show details and seat prices")
    void updateShow_Success() {
        UpdateShowRequest request = new UpdateShowRequest(LocalDateTime.now().plusDays(1), BigDecimal.valueOf(300));
        ShowSeat showSeat = ShowSeat.builder().price(BigDecimal.valueOf(250)).build();

        when(showRepository.findById(1L)).thenReturn(Optional.of(mockShow));
        when(showSeatRepository.findByShowId(1L)).thenReturn(List.of(showSeat));

        showService.updateShow(1L, request);

        assertThat(mockShow.getPrice()).isEqualTo(BigDecimal.valueOf(300));
        assertThat(showSeat.getPrice()).isEqualTo(BigDecimal.valueOf(300));
        verify(showRepository).findById(1L);
    }

    @Test
    @DisplayName("Should delete show and its seats")
    void deleteShow_Success() {
        when(showRepository.findById(1L)).thenReturn(Optional.of(mockShow));

        showService.deleteShow(1L);

        verify(showSeatRepository).deleteByShowId(1L);
        verify(showRepository).delete(mockShow);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when show ID is invalid")
    void deleteShow_NotFound() {
        when(showRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> showService.deleteShow(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Show not found");
    }

    @Test
    @DisplayName("Should get all shows as responses")
    void getAllShows_Success() {
        when(showRepository.findAll()).thenReturn(List.of(mockShow));

        List<ShowResponse> result = showService.getAllShows();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).movieTitle()).isEqualTo("Inception");
    }

    @Test
    @DisplayName("Should get all seats for a specific show")
    void getSeatsForShow_Success() {
        Seat seat = Seat.builder().seatNumber("A1").seatType(SeatType.PREMIUM).build();
        ShowSeat showSeat = ShowSeat.builder().id(1L).seat(seat).booked(false).price(BigDecimal.valueOf(250)).build();
        when(showSeatRepository.findByShowId(1L)).thenReturn(List.of(showSeat));

        List<ShowSeatResponse> result = showService.getSeatsForShow(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSeatNumber()).isEqualTo("A1");
        verify(showSeatRepository).findByShowId(1L);
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Should filter shows based on criteria")
    void getShows_WithFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Show> showPage = new PageImpl<>(List.of(mockShow));
        when(showRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(showPage);

        Page<ShowResponse> result = showService.getShows(LocalDate.now(), 1L, 1L, "chennai", pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(showRepository).findAll(any(Specification.class), eq(pageable));
    }
}
