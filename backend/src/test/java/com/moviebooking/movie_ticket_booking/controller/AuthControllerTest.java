package com.moviebooking.movie_ticket_booking.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.web.servlet.MockMvc;

import com.moviebooking.movie_ticket_booking.dto.LoginResponse;
import com.moviebooking.movie_ticket_booking.security.JwtAuthenticationFilter;
import com.moviebooking.movie_ticket_booking.service.AuthService;
import com.moviebooking.movie_ticket_booking.service.UserService;

@WebMvcTest(
    controllers = AuthController.class,
    excludeAutoConfiguration = SecurityAutoConfiguration.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = JwtAuthenticationFilter.class
    )
)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {
    
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthService authService;

    // POST /auth/register

    @Test
    void register_returnsOkAndMessage_andCallsUserService() throws Exception {
        String json = """
            {
              "name": "User",
              "email": "user@test.com",
              "password": "pass123"
            }
            """;

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isOk())
            .andExpect(content().string("User registered successfully"));

        verify(userService, times(1)).register(any());
        verifyNoInteractions(authService);
    }

    @Test
    void register_whenUserServiceThrows_returns500() throws Exception {
        doThrow(new RuntimeException("Email already registered"))
                .when(userService).register(any());

        String json = """
            {
              "name": "User",
              "email": "user@test.com",
              "password": "pass123"
            }
            """;

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isInternalServerError());

        verify(userService, times(1)).register(any());
        verifyNoInteractions(authService);
    }

    // POST /auth/login

    @Test
    void login_returnsTokenJson_andCallsAuthService() throws Exception {
        when(authService.login(any()))
                .thenReturn(new LoginResponse("jwt-token"));

        String json = """
            {
              "email": "user@test.com",
              "password": "pass123"
            }
            """;

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.token").value("jwt-token"));

        verify(authService, times(1)).login(any());
        verifyNoInteractions(userService);
    }

    @Test
    void login_whenAuthServiceThrows_returns500() throws Exception {
        when(authService.login(any()))
                .thenThrow(new RuntimeException("User not found"));

        String json = """
            {
              "email": "missing@test.com",
              "password": "pass123"
            }
            """;

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isInternalServerError());

        verify(authService, times(1)).login(any());
        verifyNoInteractions(userService);
    }
}
