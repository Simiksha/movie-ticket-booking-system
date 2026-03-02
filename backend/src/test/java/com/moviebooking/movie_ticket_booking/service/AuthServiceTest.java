package com.moviebooking.movie_ticket_booking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.moviebooking.movie_ticket_booking.dto.LoginRequest;
import com.moviebooking.movie_ticket_booking.dto.LoginResponse;
import com.moviebooking.movie_ticket_booking.model.Role;
import com.moviebooking.movie_ticket_booking.model.User;
import com.moviebooking.movie_ticket_booking.repository.UserRepository;
import com.moviebooking.movie_ticket_booking.security.JwtUtil;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtil jwtUtil;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_success_authenticates_fetchesUser_generatesToken_returnsResponse() {
        // Arrange
        LoginRequest request = new LoginRequest("user@test.com", "pass123");

        User user = User.builder()
                .id(1L)
                .email("user@test.com")
                .role(Role.USER)
                .build();

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken("user@test.com", "USER")).thenReturn("jwt-token");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("user@test.com", "pass123"));

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());

        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);

        verify(authenticationManager, times(1)).authenticate(captor.capture());
        UsernamePasswordAuthenticationToken authToken = captor.getValue();

        assertEquals("user@test.com", authToken.getPrincipal());
        assertEquals("pass123", authToken.getCredentials());

        verify(userRepository).findByEmail("user@test.com");
        verify(jwtUtil).generateToken("user@test.com", "USER");
    }

    @Test
    void login_badCredentials_throws_andDoesNotCallRepositoryOrJwt() {
        LoginRequest request = new LoginRequest("user@test.com", "wrong");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));

        verifyNoInteractions(userRepository);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void login_userNotFound_throws_andDoesNotGenerateToken() {
        LoginRequest request = new LoginRequest("missing@test.com", "pass");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("missing@test.com", "pass"));

        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        // Act + Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(request));
        assertEquals("User not found", ex.getMessage());

        verify(userRepository).findByEmail("missing@test.com");
        verifyNoInteractions(jwtUtil);
    }
}
