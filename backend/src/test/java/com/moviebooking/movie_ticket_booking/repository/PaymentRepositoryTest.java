package com.moviebooking.movie_ticket_booking.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.moviebooking.movie_ticket_booking.model.Booking;
import com.moviebooking.movie_ticket_booking.model.BookingStatus;
import com.moviebooking.movie_ticket_booking.model.Genre;
import com.moviebooking.movie_ticket_booking.model.Movie;
import com.moviebooking.movie_ticket_booking.model.Payment;
import com.moviebooking.movie_ticket_booking.model.PaymentStatus;
import com.moviebooking.movie_ticket_booking.model.Role;
import com.moviebooking.movie_ticket_booking.model.Show;
import com.moviebooking.movie_ticket_booking.model.User;
import com.moviebooking.movie_ticket_booking.theater.Screen;
import com.moviebooking.movie_ticket_booking.theater.Theater;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
public class PaymentRepositoryTest {
    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldExistByBooking() {
        Booking booking = createAndPersistBooking();
        Payment payment = createPayment(booking, "order_123", PaymentStatus.INITIATED);
        entityManager.persist(payment);

        boolean exists = paymentRepository.existsByBooking(booking);

        assertThat(exists).isTrue();
    }

    private Payment createPayment(Booking booking, String gatewayId, PaymentStatus status) {
        return Payment.builder()
                .booking(booking)
                .amount(booking.getTotalAmount())
                .status(status)
                .gatewayOrderId(gatewayId)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private Booking createAndPersistBooking() {
        // 1. Setup User
        User user = User.builder()
                .name("Tester")
                .email("test" + System.currentTimeMillis() + "@t.com")
                .password("p")
                .role(Role.USER)
                .build();
        entityManager.persist(user);

        // 2. Setup Genre & Movie
        Genre action = Genre.builder().name("Action").build();
        entityManager.persist(action);

        Movie movie = Movie.builder()
                .title("Test Movie")
                .duration(120)
                .language("English")
                .rating("UA")
                .releaseDate(LocalDate.now())
                .genres(Set.of(action))
                .active(true)
                .build();
        entityManager.persist(movie);

        // 3. Setup Theater & Screen
        Theater theater = Theater.builder()
                .name("Grand Cinema")
                .city("Downtown")
                .address("address")
                .build();
        entityManager.persist(theater);

        Screen screen = Screen.builder()
                .name("Screen 1")
                .totalSeats(100)  
                .theater(theater) 
                .build();
        entityManager.persist(screen);

        // 4. Setup Show
        Show show = Show.builder()
                .movie(movie)
                .screen(screen)
                .showTime(LocalDateTime.now())
                .price(BigDecimal.valueOf(250))
                .build();
        entityManager.persist(show);

        // 5. Setup Booking
        Booking booking = Booking.builder()
                .user(user)
                .show(show)
                .status(BookingStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(200))
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
        
        return entityManager.persist(booking);
    }

    @Test
    void shouldFindByGatewayOrderId() {
        // Given
        Booking booking = createAndPersistBooking();
        String orderId = "rzp_order_999";
        Payment payment = createPayment(booking, orderId, PaymentStatus.INITIATED);
        entityManager.persist(payment);

        // When
        Optional<Payment> found = paymentRepository.findByGatewayOrderId(orderId);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getGatewayOrderId()).isEqualTo(orderId);
    }

    @Test
    void shouldReturnEmptyWhenGatewayOrderIdNotFound() {
        // When
        Optional<Payment> found = paymentRepository.findByGatewayOrderId("non_existent_id");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void shouldFindByBooking() {
        // Given
        Booking booking = createAndPersistBooking();
        Payment payment = createPayment(booking, "order_555", PaymentStatus.SUCCESS);
        entityManager.persist(payment);

        // When
        Optional<Payment> found = paymentRepository.findByBooking(booking);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getBooking().getId()).isEqualTo(booking.getId());
    }
}
