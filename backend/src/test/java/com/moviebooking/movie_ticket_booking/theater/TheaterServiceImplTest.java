package com.moviebooking.movie_ticket_booking.theater;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.moviebooking.movie_ticket_booking.exception.BadRequestException;
import com.moviebooking.movie_ticket_booking.exception.ResourceNotFoundException;
import com.moviebooking.movie_ticket_booking.theater.dto.CreateScreenRequest;
import com.moviebooking.movie_ticket_booking.theater.dto.CreateTheaterRequest;
import com.moviebooking.movie_ticket_booking.theater.dto.ScreenResponse;
import com.moviebooking.movie_ticket_booking.theater.dto.TheaterResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class TheaterServiceImplTest {

    @Mock
    TheaterRepository theaterRepository;
    @Mock
    ScreenRepository screenRepository;
    @Mock
    SeatRepository seatRepository;

    @InjectMocks
    TheaterServiceImpl theaterService;

    private Theater theater;

    @BeforeEach
    void setup() {
        theater = Theater.builder()
                .name("PVR")
                .city("Chennai")
                .address("Some address")
                .active(true)
                .build();
        theater.setId(10L);
    }

    @Test
    void getAllTheaters_mapsEntitiesToResponses() {
        Theater t1 = Theater.builder().name("PVR").city("Chennai").build();
        t1.setId(1L);
        Theater t2 = Theater.builder().name("INOX").city("Bangalore").build();
        t2.setId(2L);

        when(theaterRepository.findAll()).thenReturn(List.of(t1, t2));

        List<TheaterResponse> out = theaterService.getAllTheaters();

        assertThat(out).hasSize(2);
        assertThat(out.get(0).id()).isEqualTo(1L);
        assertThat(out.get(0).name()).isEqualTo("PVR");
        assertThat(out.get(0).city()).isEqualTo("Chennai");
        assertThat(out.get(1).id()).isEqualTo(2L);

        verify(theaterRepository).findAll();
    }

    @Test
    void createTheater_whenDuplicate_throwsBadRequest() {
        CreateTheaterRequest req = new CreateTheaterRequest("PVR", "Chennai", "Addr");
        when(theaterRepository.findByNameAndCity("PVR", "Chennai"))
                .thenReturn(Optional.of(theater));

        assertThatThrownBy(() -> theaterService.createTheater(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Theater already exists");

        verify(theaterRepository, never()).save(any());
    }

    @Test
    void createTheater_success_savesAndReturnsId() {
        CreateTheaterRequest req = new CreateTheaterRequest("PVR", "Chennai", "Addr");
        when(theaterRepository.findByNameAndCity("PVR", "Chennai"))
                .thenReturn(Optional.empty());

        when(theaterRepository.save(any(Theater.class))).thenAnswer(inv -> {
            Theater saved = inv.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        Long id = theaterService.createTheater(req);

        assertThat(id).isEqualTo(99L);

        ArgumentCaptor<Theater> captor = ArgumentCaptor.forClass(Theater.class);
        verify(theaterRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("PVR");
        assertThat(captor.getValue().getCity()).isEqualTo("Chennai");
        assertThat(captor.getValue().isActive()).isTrue();
    }

    @Test
    void addScreen_theaterNotFound_throws() {
        when(theaterRepository.findById(10L)).thenReturn(Optional.empty());

        CreateScreenRequest req = new CreateScreenRequest("Screen 1", 100);

        assertThatThrownBy(() -> theaterService.addScreen(10L, req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Theater not found");

        verifyNoInteractions(screenRepository, seatRepository);
    }

    @Test
    void addScreen_duplicateScreen_throwsBadRequest() {
        when(theaterRepository.findById(10L)).thenReturn(Optional.of(theater));
        when(screenRepository.findByNameAndTheaterId("Screen 1", 10L))
                .thenReturn(Optional.of(Screen.builder().name("Screen 1").theater(theater).build()));

        CreateScreenRequest req = new CreateScreenRequest("Screen 1", 100);

        assertThatThrownBy(() -> theaterService.addScreen(10L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Screen already exists");

        verify(screenRepository, never()).save(any());
        verify(seatRepository, never()).saveAll(any());
    }

    @Test
    void addScreen_success_createsScreen_andAutoGeneratesSeats_withCorrectCounts() {
        when(theaterRepository.findById(10L)).thenReturn(Optional.of(theater));
        when(screenRepository.findByNameAndTheaterId("Screen 1", 10L))
                .thenReturn(Optional.empty());

        Screen savedScreen = Screen.builder()
                .name("Screen 1")
                .totalSeats(100)
                .theater(theater)
                .build();
        savedScreen.setId(50L);

        when(screenRepository.save(any(Screen.class))).thenReturn(savedScreen);
        when(seatRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        CreateScreenRequest req = new CreateScreenRequest("Screen 1", 100);

        Long screenId = theaterService.addScreen(10L, req);

        assertThat(screenId).isEqualTo(50L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<Seat>> seatsCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(seatRepository).saveAll(seatsCaptor.capture());

        List<Seat> seats = new ArrayList<>();
        seatsCaptor.getValue().forEach(seats::add);

        // assertions...
        assertThat(seats).hasSize(100);

        // verify type distribution: 70% REGULAR, next 20% PREMIUM, last 10% RECLINER
        long regular = seats.stream().filter(s -> s.getSeatType() == SeatType.REGULAR).count();
        long premium = seats.stream().filter(s -> s.getSeatType() == SeatType.PREMIUM).count();
        long recliner = seats.stream().filter(s -> s.getSeatType() == SeatType.RECLINER).count();

        assertThat(regular).isEqualTo(70);
        assertThat(premium).isEqualTo(20);
        assertThat(recliner).isEqualTo(10);

        // spot check numbering pattern
        assertThat(seats.get(0).getSeatNumber()).isEqualTo("A1");
        assertThat(seats.get(11).getSeatNumber()).isEqualTo("A12");
        assertThat(seats.get(12).getSeatNumber()).isEqualTo("B1");
    }

    @Test
    void getScreens_theaterNotFound_throws() {
        when(theaterRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> theaterService.getScreens(10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Theater not found");
    }

    @Test
    void getScreens_mapsToResponses() {
        when(theaterRepository.findById(10L)).thenReturn(Optional.of(theater));

        Screen s1 = Screen.builder().name("Screen 1").totalSeats(100).theater(theater).build();
        s1.setId(1L);
        Screen s2 = Screen.builder().name("Screen 2").totalSeats(80).theater(theater).build();
        s2.setId(2L);

        when(screenRepository.findByTheaterId(10L)).thenReturn(List.of(s1, s2));

        List<ScreenResponse> out = theaterService.getScreens(10L);

        assertThat(out).hasSize(2);
        assertThat(out.get(0).id()).isEqualTo(1L);
        assertThat(out.get(0).name()).isEqualTo("Screen 1");
        assertThat(out.get(0).totalSeats()).isEqualTo(100);
    }
}