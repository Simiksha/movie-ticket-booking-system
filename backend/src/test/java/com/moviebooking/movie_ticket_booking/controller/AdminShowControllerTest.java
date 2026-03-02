package com.moviebooking.movie_ticket_booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moviebooking.movie_ticket_booking.dto.*;
import com.moviebooking.movie_ticket_booking.show.ShowService;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminShowController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminShowControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @MockitoBean ShowService showService;

    //@MockitoBean JwtAuthFilter jwtAuthFilter;          
    @MockitoBean UserDetailsService userDetailsService;

    @Test
    void createShow_returnsId() throws Exception {
        when(showService.createShow(any(CreateShowRequest.class))).thenReturn(101L);

        // Don’t construct DTOs (builder/ctor issues) — send JSON directly
        String body = """
                {
                  "movieId": 1,
                  "screenId": 2,
                  "showDate": "2026-03-01",
                  "showTime": "18:30",
                  "basePrice": 250
                }
                """;

        mvc.perform(post("/admin/shows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string("101"));

        verify(showService, times(1)).createShow(any(CreateShowRequest.class));
    }

    @Test
    void updateShow_returns200_andCallsService() throws Exception {
        String body = """
                {
                  "movieId": 1,
                  "screenId": 2,
                  "showDate": "2026-03-02",
                  "showTime": "20:00",
                  "basePrice": 300
                }
                """;

        mvc.perform(put("/admin/shows/{id}", 5)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(showService, times(1)).updateShow(eq(5L), any(UpdateShowRequest.class));
    }

    @Test
    void deleteShow_returns200_andCallsService() throws Exception {
        mvc.perform(delete("/admin/shows/{id}", 9))
                .andExpect(status().isOk());

        verify(showService, times(1)).deleteShow(9L);
    }

    @Test
    void getAllShows_returnsList() throws Exception {
        ShowResponse s1 = Mockito.mock(ShowResponse.class);
        ShowResponse s2 = Mockito.mock(ShowResponse.class);
        when(showService.getAllShows()).thenReturn(List.of(s1, s2));

        mvc.perform(get("/admin/shows"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                // only assert size to avoid needing real ShowResponse constructor
                .andExpect(jsonPath("$.length()").value(2));

        verify(showService, times(1)).getAllShows();
    }

    @Test
    void bulkCreateShows_returnsCreatedCount() throws Exception {
        when(showService.bulkCreateShows(any(BulkCreateShowsRequest.class))).thenReturn(7);

        String body = """
                {
                  "movieId": 1,
                  "theaterId": 2,
                  "screenId": 3,
                  "startDate": "2026-03-01",
                  "endDate": "2026-03-03",
                  "showTimes": ["10:00", "14:00", "18:00"],
                  "basePrice": 250
                }
                """;

        mvc.perform(post("/admin/shows/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string("7"));

        verify(showService, times(1)).bulkCreateShows(any(BulkCreateShowsRequest.class));
    }

    @Test
    void createShow_invalidBody_returns400_andDoesNotCallService() throws Exception {
        // empty JSON -> should fail @Valid (assuming you have @NotNull on fields)
        String body = "{}";

        mvc.perform(post("/admin/shows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(showService, never()).createShow(any());
    }
}