package com.moviebooking.movie_ticket_booking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.moviebooking.movie_ticket_booking.dto.RegisterRequest;
import com.moviebooking.movie_ticket_booking.model.Role;
import com.moviebooking.movie_ticket_booking.model.User;
import com.moviebooking.movie_ticket_booking.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void register_success_encodesPassword_setsDefaults_andSavesUser() {
        RegisterRequest request = RegisterRequest.builder()
                .name("testuser")
                .email("testuser@test.com")
                .password("password")
                .build();

        when(userRepository.existsByEmail("testuser@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("password");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        userService.register(request);

        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertNotNull(saved);
        assertEquals("testuser", saved.getName());
        assertEquals("testuser@test.com", saved.getEmail());
        assertEquals("password", saved.getPassword());
        assertEquals(Role.USER, saved.getRole());
        assertTrue(saved.isEnabled());

        verify(passwordEncoder).encode("password");
    }

    @Test
    void register_emailAlreadyRegistered_throws_andDoesNotEncodeOrSave() {
        RegisterRequest request = RegisterRequest.builder()
                .name("testuser")
                .email("testuser@test.com")
                .password("password")
                .build();

        when(userRepository.existsByEmail("testuser@test.com")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.register(request));
        assertEquals("Email already registered", ex.getMessage());

        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).save(any(User.class));
    }
}
