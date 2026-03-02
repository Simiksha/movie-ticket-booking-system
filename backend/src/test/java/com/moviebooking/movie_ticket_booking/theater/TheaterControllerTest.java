package com.moviebooking.movie_ticket_booking.theater;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moviebooking.movie_ticket_booking.config.JwtProperties;
import com.moviebooking.movie_ticket_booking.config.RazorpayProperties;
import com.moviebooking.movie_ticket_booking.config.SecurityBeansConfig;
import com.moviebooking.movie_ticket_booking.config.SecurityConfig;
import com.moviebooking.movie_ticket_booking.security.CustomUserDetailsService;
import com.moviebooking.movie_ticket_booking.security.JwtAuthenticationFilter;
import com.moviebooking.movie_ticket_booking.security.JwtUtil;
import com.moviebooking.movie_ticket_booking.theater.dto.CreateScreenRequest;
import com.moviebooking.movie_ticket_booking.theater.dto.CreateTheaterRequest;

@WebMvcTest(TheaterController.class)
@Import({SecurityConfig.class, SecurityBeansConfig.class})
public class TheaterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TheaterService theaterService;

    
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private JwtProperties jwtProperties;

    @MockitoBean
    private RazorpayProperties razorpayProperties;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "ADMIN") 
    void shouldCreateTheaterSuccessfully() throws Exception {
        CreateTheaterRequest request = new CreateTheaterRequest("PVR", "Mumbai", "Main St");
        when(theaterService.createTheater(any())).thenReturn(1L);

        mockMvc.perform(post("/admin/theaters")
                        .with(csrf()) 
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAddScreenToTheater() throws Exception {
        Long theaterId = 1L;
        CreateScreenRequest request = new CreateScreenRequest("IMAX", 250);
        when(theaterService.addScreen(eq(theaterId), any(CreateScreenRequest.class))).thenReturn(10L);

        mockMvc.perform(post("/admin/theaters/{theaterId}/screens", theaterId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("10"));
    }

    @Test
    @WithMockUser(roles = "USER") 
    void shouldReturnForbiddenForNonAdmin() throws Exception {
        CreateTheaterRequest request = new CreateTheaterRequest("PVR", "Mumbai", "Main St");

        mockMvc.perform(post("/admin/theaters")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
