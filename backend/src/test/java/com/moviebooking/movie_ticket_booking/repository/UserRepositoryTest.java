package com.moviebooking.movie_ticket_booking.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.moviebooking.movie_ticket_booking.model.Role;
import com.moviebooking.movie_ticket_booking.model.User;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;


@DataJpaTest
public class UserRepositoryTest {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager; 

    @Test
    void shouldFindByEmail() {
        // Given
        User user = User.builder()
                .name("Alice")
                .email("alice@example.com")
                .password("securePass")
                .role(Role.USER)
                .build();
        entityManager.persistAndFlush(user);

        // When
        Optional<User> found = userRepository.findByEmail("alice@example.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Alice");
    }

    @Test
    void shouldReturnEmptyWhenEmailDoesNotExist() {
        // When
        Optional<User> found = userRepository.findByEmail("notfound@example.com");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void shouldReturnTrueWhenEmailExists() {
        // Given
        User user = User.builder()
                .name("Bob")
                .email("bob@example.com")
                .password("pass123")
                .role(Role.USER)
                .build();
        entityManager.persistAndFlush(user);

        // When
        boolean exists = userRepository.existsByEmail("bob@example.com");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void shouldReturnFalseWhenEmailDoesNotExist() {
        // When
        boolean exists = userRepository.existsByEmail("unknown@example.com");

        // Then
        assertThat(exists).isFalse();
    }
}
