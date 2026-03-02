package com.moviebooking.movie_ticket_booking.service;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import com.moviebooking.movie_ticket_booking.dto.BookingHistoryResponse;
import com.moviebooking.movie_ticket_booking.dto.BookingRequest;
import com.moviebooking.movie_ticket_booking.exception.BadRequestException;
import com.moviebooking.movie_ticket_booking.model.Booking;
import com.moviebooking.movie_ticket_booking.model.BookingSeat;
import com.moviebooking.movie_ticket_booking.model.BookingStatus;
import com.moviebooking.movie_ticket_booking.model.Movie;
import com.moviebooking.movie_ticket_booking.model.Show;
import com.moviebooking.movie_ticket_booking.model.User;
import com.moviebooking.movie_ticket_booking.repository.BookingRepository;
import com.moviebooking.movie_ticket_booking.repository.BookingSeatRepository;
import com.moviebooking.movie_ticket_booking.repository.PaymentRepository;
import com.moviebooking.movie_ticket_booking.repository.ShowRepository;
import com.moviebooking.movie_ticket_booking.repository.UserRepository;
import com.moviebooking.movie_ticket_booking.theater.Screen;
import com.moviebooking.movie_ticket_booking.theater.Seat;
import com.moviebooking.movie_ticket_booking.theater.ShowSeat;
import com.moviebooking.movie_ticket_booking.theater.ShowSeatRepository;
import com.moviebooking.movie_ticket_booking.theater.Theater;


@ExtendWith(MockitoExtension.class)
public class BookingServiceTest {


    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BookingSeatRepository bookingSeatRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ShowSeatRepository showSeatRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private ShowRepository showRepository;


    @InjectMocks
    private BookingService bookingService;


    private User user;
    private Movie movie;
    private Theater theater;
    private Screen screen;
    private Show show;
    private Seat seat1;
    private Seat seat2;
    private ShowSeat ss1;
    private ShowSeat ss2;


    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("a@b.com").build();


        movie = Movie.builder().id(10L).title("Interstellar").build();
        theater = Theater.builder().id(20L).name("PVR").build();
        screen = Screen.builder().id(30L).theater(theater).build();


        show = Show.builder()
                .id(100L)
                .movie(movie)
                .screen(screen)
                .showTime(LocalDateTime.of(2026, 2, 22, 18, 30))
                .build();


        user = new User();
        user.setId(1L);
        user.setEmail("u@test.com");


        // minimal show graph for createBooking
        show = new Show();
        show.setId(10L);
        show.setShowTime(LocalDateTime.now().plusHours(2)); // not started


        seat1 = new Seat();
        seat1.setId(100L);
        seat1.setSeatNumber("A1");


        seat2 = new Seat();
        seat2.setId(101L);
        seat2.setSeatNumber("A2");


        ss1 = new ShowSeat();
        ss1.setId(1000L);
        ss1.setSeat(seat1);
        ss1.setPrice(new BigDecimal("250"));
        ss1.setBooked(false);


        ss2 = new ShowSeat();
        ss2.setId(1001L);
        ss2.setSeat(seat2);
        ss2.setPrice(new BigDecimal("250"));
        ss2.setBooked(false);
    }


    // createBooking() tests


    @Test
    void createBooking_success_marksSeatsBooked_savesBookingSeats_andReturnsBookingId() {
        // arrange
        when(userRepository.findByEmail("u@test.com")).thenReturn(Optional.of(user));
        when(showRepository.findById(10L)).thenReturn(Optional.of(show));


        List<Long> seatIds = List.of(100L, 101L);
        when(showSeatRepository.findByShowIdAndSeatIdInForUpdate(10L, seatIds))
                .thenReturn(List.of(ss1, ss2));


        // emulate JPA id generation
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            if (b.getId() == null) b.setId(999L);
            return b;
        });


        BookingRequest req = mock(BookingRequest.class);
        when(req.getShowId()).thenReturn(10L);
        when(req.getSeatIds()).thenReturn(seatIds);


        // act
        Long bookingId = bookingService.createBooking("u@test.com", req);


        // assert
        assertThat(bookingId).isEqualTo(999L);
        assertThat(ss1.isBooked()).isTrue();
        assertThat(ss2.isBooked()).isTrue();


        verify(bookingSeatRepository, times(2)).save(any(BookingSeat.class));


        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository, atLeastOnce()).save(bookingCaptor.capture());
        Booking saved = bookingCaptor.getAllValues().get(bookingCaptor.getAllValues().size() - 1);


        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getShow()).isSameAs(show);
        assertThat(saved.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(saved.getTotalAmount()).isEqualByComparingTo(new BigDecimal("500"));
        assertThat(saved.getExpiresAt()).isNotNull();
    }


    @Test
    void createBooking_userNotFound_throws() {
        when(userRepository.findByEmail("x@y.com")).thenReturn(Optional.empty());


        BookingRequest request = BookingRequest.builder()
                .showId(show.getId())
                .seatIds(List.of(1L))
                .build();


        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.createBooking("x@y.com", request));


        assertEquals("User not found", ex.getMessage());
        verifyNoInteractions(showSeatRepository, bookingRepository, bookingSeatRepository);
    }



    @Test
    void createBooking_noSeatsSelected_throws() {
        when(userRepository.findByEmail("u@test.com")).thenReturn(Optional.of(user));


        BookingRequest req = mock(BookingRequest.class);
        when(req.getShowId()).thenReturn(10L);
        when(req.getSeatIds()).thenReturn(Collections.emptyList());


        assertThatThrownBy(() -> bookingService.createBooking("u@test.com", req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No seats selected");


        verifyNoInteractions(showRepository, showSeatRepository, bookingRepository);
    }


    @Test
    void createBooking_showStarted_throwsBadRequest() {
        show.setShowTime(LocalDateTime.now().minusMinutes(1)); // started
        when(userRepository.findByEmail("u@test.com")).thenReturn(Optional.of(user));
        when(showRepository.findById(10L)).thenReturn(Optional.of(show));


        BookingRequest req = mock(BookingRequest.class);
        when(req.getShowId()).thenReturn(10L);
        when(req.getSeatIds()).thenReturn(List.of(100L));


        assertThatThrownBy(() -> bookingService.createBooking("u@test.com", req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Show already started");


        verifyNoInteractions(showSeatRepository);
        verify(bookingRepository, never()).save(any());
    }


    @Test
    void createBooking_seatAlreadyBooked_throws() {
        ss1.setBooked(true);


        when(userRepository.findByEmail("u@test.com")).thenReturn(Optional.of(user));
        when(showRepository.findById(10L)).thenReturn(Optional.of(show));
        when(showSeatRepository.findByShowIdAndSeatIdInForUpdate(eq(10L), anyList()))
                .thenReturn(List.of(ss1));


        BookingRequest req = mock(BookingRequest.class);
        when(req.getShowId()).thenReturn(10L);
        when(req.getSeatIds()).thenReturn(List.of(100L));


        assertThatThrownBy(() -> bookingService.createBooking("u@test.com", req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Seat already booked")
                .hasMessageContaining("A1");


        verify(bookingRepository, never()).save(any());
    }


    @Test
    void confirmBooking_success_setsConfirmed_andSendsEmail() {
        Booking booking = new Booking();
        booking.setId(55L);
        booking.setUser(user);
        booking.setShow(show);
        booking.setStatus(BookingStatus.PENDING);
        booking.setExpiresAt(LocalDateTime.now().plusMinutes(5));


        // NOTE: confirmBooking builds email content -> needs movie/theater graph + bookingSeats
        Movie movie = new Movie();
        movie.setTitle("Interstellar");
        Theater theater = new Theater();
        theater.setName("PVR");
        Screen screen = new Screen();
        screen.setName("Screen 1");
        screen.setTheater(theater);
        show.setMovie(movie);
        show.setScreen(screen);


        BookingSeat bs1 = new BookingSeat();
        bs1.setBooking(booking);
        bs1.setShowSeat(ss1);


        booking.setBookingSeats(List.of(bs1));


        when(bookingRepository.findById(55L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));


        bookingService.confirmBooking(55L);


        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);


        verify(bookingRepository).save(booking);
        verify(emailService, times(1)).sendBookingConfirmation(
                eq("u@test.com"),
                contains("Booking Confirmed"),
                contains("Interstellar")
        );
    }


    @Test
    void confirmBooking_notPending_throws() {
        Booking booking = new Booking();
        booking.setId(55L);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setExpiresAt(LocalDateTime.now().plusMinutes(5));


        when(bookingRepository.findById(55L)).thenReturn(Optional.of(booking));


        assertThatThrownBy(() -> bookingService.confirmBooking(55L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not pending");


        verify(emailService, never()).sendBookingConfirmation(anyString(), anyString(), anyString());
        verify(bookingRepository, never()).save(any());
    }


    @Test
    void confirmBooking_expired_throws() {
        Booking booking = new Booking();
        booking.setId(55L);
        booking.setStatus(BookingStatus.PENDING);
        booking.setExpiresAt(LocalDateTime.now().minusSeconds(1));


        when(bookingRepository.findById(55L)).thenReturn(Optional.of(booking));


        assertThatThrownBy(() -> bookingService.confirmBooking(55L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("expired");


        verify(emailService, never()).sendBookingConfirmation(anyString(), anyString(), anyString());
        verify(bookingRepository, never()).save(any());
    }


    @Test
    void expirePendingBookings_marksExpired_andReleasesSeats() {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setStatus(BookingStatus.PENDING);
        booking.setExpiresAt(LocalDateTime.now().minusMinutes(1));


        ShowSeat bookedSeat = new ShowSeat();
        bookedSeat.setSeat(seat1);
        bookedSeat.setBooked(true);


        BookingSeat bs = new BookingSeat();
        bs.setShowSeat(bookedSeat);
        booking.setBookingSeats(List.of(bs));


        when(bookingRepository.findAllByStatusAndExpiresAtBefore(eq(BookingStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of(booking));


        bookingService.expirePendingBookings();


        assertThat(booking.getStatus()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(bookedSeat.isBooked()).isFalse();
        verify(bookingRepository).saveAll(any());
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.EXPIRED);
    }


    @Test
    void getMyBookings_returnsHistoryResponses_sortedByRepo() {
        when(userRepository.findByEmail("u@test.com")).thenReturn(Optional.of(user));


        Booking b1 = new Booking();
        b1.setId(1L);
        b1.setUser(user);
        b1.setStatus(BookingStatus.CONFIRMED);
        b1.setCreatedAt(LocalDateTime.now());
        b1.setTotalAmount(new BigDecimal("250"));
        b1.setShow(show);


        // minimal graph for convertToHistoryResponse
        Movie movie = new Movie();
        movie.setTitle("Interstellar");
        Theater theater = new Theater();
        theater.setName("PVR");
        Screen screen = new Screen();
        screen.setName("Screen 1");
        screen.setTheater(theater);
        show.setMovie(movie);
        show.setScreen(screen);


        BookingSeat bs = new BookingSeat();
        bs.setShowSeat(ss1);
        b1.setBookingSeats(List.of(bs));


        when(bookingRepository.findByUserOrderByCreatedAtDesc(user)).thenReturn(List.of(b1));
        when(paymentRepository.findByBooking(b1)).thenReturn(Optional.empty());


        List<BookingHistoryResponse> out = bookingService.getMyBookings("u@test.com");


        assertThat(out).hasSize(1);
        assertThat(out.get(0).getBookingId()).isEqualTo(1L);
        assertThat(out.get(0).getMovieTitle()).isEqualTo("Interstellar");
        assertThat(out.get(0).getTheaterName()).isEqualTo("PVR");
        assertThat(out.get(0).getPaymentStatus()).isEqualTo("NOT_PAID");
    }
}
