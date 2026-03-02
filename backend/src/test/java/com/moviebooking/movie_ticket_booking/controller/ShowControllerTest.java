package com.moviebooking.movie_ticket_booking.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.moviebooking.movie_ticket_booking.config.SecurityConfig;
import com.moviebooking.movie_ticket_booking.dto.ShowResponse;
import com.moviebooking.movie_ticket_booking.dto.ShowSeatResponse;
import com.moviebooking.movie_ticket_booking.security.JwtAuthenticationFilter;
import com.moviebooking.movie_ticket_booking.show.ShowService;

@WebMvcTest(
    controllers = ShowController.class,
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class)
    }
)
@AutoConfigureMockMvc(addFilters = false)
public class ShowControllerTest {
    
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShowService showService;

    // GET /shows/{showId}/seats
    @Test
    void getSeats_returnsList() throws Exception {

        ShowSeatResponse seat1 =
                new ShowSeatResponse(1L, 1L, "A1", "REGULAR", false, BigDecimal.valueOf(200));

        ShowSeatResponse seat2 =
                new ShowSeatResponse(2L, 2L, "A2", "REGULAR", true, BigDecimal.valueOf(200));

        when(showService.getSeatsForShow(10L))
                .thenReturn(List.of(seat1, seat2));

        mockMvc.perform(get("/shows/{showId}/seats", 10L))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].seatNumber").value("A1"))
                .andExpect(jsonPath("$[1].booked").value(true))
                .andExpect(jsonPath("$[1].price").value(200.0));

        verify(showService).getSeatsForShow(10L);
    }

    // GET /shows (no filters)
    @Test
    void getShows_withoutFilters_returnsPage() throws Exception {

        ShowResponse r1 = ShowResponse.builder()
                .id(1L)
                .movieTitle("Movie A")
                .screenName("Screen 1")
                .theaterName("Theater X")
                .showTime(LocalDateTime.parse("2026-02-23T18:30:00"))
                .price(BigDecimal.valueOf(250))
                .build();

        PageRequest pageable = PageRequest.of(0, 10);
        Page<ShowResponse> page =
                new PageImpl<>(List.of(r1), pageable, 1);

        when(showService.getShows(isNull(), isNull(), isNull(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/shows")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(showService).getShows(isNull(), isNull(), isNull(), any());
    }

    // GET /shows with filters
    @Test
    void getShows_withFilters_callsServiceCorrectly() throws Exception {

        LocalDate date = LocalDate.parse("2026-02-23");

        Page<ShowResponse> empty =
                new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        when(showService.getShows(eq(date), eq(5L), eq(7L), any()))
                .thenReturn(empty);

        mockMvc.perform(get("/shows")
                        .param("date", "2026-02-23")
                        .param("theaterId", "5")
                        .param("movieId", "7")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));

        verify(showService).getShows(eq(date), eq(5L), eq(7L), any());
    }

    // Invalid date format
    @Test
    void getShows_invalidDate_returns400() throws Exception {

        mockMvc.perform(get("/shows")
                        .param("date", "23-02-2026")) 
                .andExpect(status().isBadRequest());
    }
}
