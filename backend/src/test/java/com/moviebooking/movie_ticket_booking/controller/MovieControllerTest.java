package com.moviebooking.movie_ticket_booking.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

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
import com.moviebooking.movie_ticket_booking.dto.CreateMovieRequest;
import com.moviebooking.movie_ticket_booking.dto.MovieResponse;
import com.moviebooking.movie_ticket_booking.dto.UpdateMovieRequest;
import com.moviebooking.movie_ticket_booking.security.JwtAuthenticationFilter;
import com.moviebooking.movie_ticket_booking.service.MovieService;

@WebMvcTest(
        controllers = MovieController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
public class MovieControllerTest {
    
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MovieService movieService;

    @Test
    void getMovies_withoutGenre_callsGetAllMovies_andReturnsPageJson() throws Exception {
        MovieResponse m1 = MovieResponse.builder()
                .id(1L)
                .title("Interstellar")
                .description("Sci-fi")
                .genres(Set.of("SCI-FI"))
                .duration(169)
                .language("EN")
                .rating("PG-13")
                .releaseDate(LocalDate.parse("2014-11-07"))
                .posterUrl("url1")
                .build();

        Page<MovieResponse> page = new PageImpl<>(
                List.of(m1),
                PageRequest.of(0, 20),
                1
        );

        when(movieService.getAllMovies(any())).thenReturn(page);

        mockMvc.perform(get("/movies")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Interstellar"))
                .andExpect(jsonPath("$.content[0].genres[0]").value("SCI-FI"));

        verify(movieService).getAllMovies(any());
        verifyNoMoreInteractions(movieService);
    }

    @Test
    void getMovies_withGenre_callsGetMoviesByGenre_andReturnsPageJson() throws Exception {
        MovieResponse m1 = MovieResponse.builder()
                .id(10L)
                .title("Mad Max")
                .genres(Set.of("ACTION"))
                .duration(120)
                .language("EN")
                .rating("R")
                .releaseDate(LocalDate.parse("2015-05-15"))
                .build();

        Page<MovieResponse> page = new PageImpl<>(
                List.of(m1),
                PageRequest.of(1, 5),
                1
        );

        when(movieService.getMoviesByGenre(eq("ACTION"), any())).thenReturn(page);

        mockMvc.perform(get("/movies")
                        .param("genre", "ACTION")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].title").value("Mad Max"));

        verify(movieService).getMoviesByGenre(eq("ACTION"), any());
        verifyNoMoreInteractions(movieService);
    }

    @Test
    void createMovie_validRequest_returnsMovieResponse() throws Exception {
        MovieResponse created = MovieResponse.builder()
                .id(5L)
                .title("Inception")
                .genres(Set.of("SCI-FI", "THRILLER"))
                .duration(148)
                .language("EN")
                .rating("PG-13")
                .releaseDate(LocalDate.parse("2010-07-16"))
                .posterUrl("poster")
                .build();

        when(movieService.createMovie(any(CreateMovieRequest.class))).thenReturn(created);

        String json = """
                {
                  "title": "Inception",
                  "description": "Dreams",
                  "genres": ["SCI-FI", "THRILLER"],
                  "duration": 148,
                  "language": "EN",
                  "rating": "PG-13",
                  "releaseDate": "2010-07-16",
                  "posterUrl": "poster"
                }
                """;

        mockMvc.perform(post("/admin/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.title").value("Inception"))
                .andExpect(jsonPath("$.duration").value(148));

        verify(movieService).createMovie(any(CreateMovieRequest.class));
        verifyNoMoreInteractions(movieService);
    }

    @Test
    void createMovie_invalidRequest_returns400_andDoesNotCallService() throws Exception {
        String json = """
                {
                  "description": "Missing required fields"
                }
                """;

        mockMvc.perform(post("/admin/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verifyNoMoreInteractions(movieService);
    }

    @Test
    void updateMovie_returnsUpdatedMovieResponse() throws Exception {
        MovieResponse updated = MovieResponse.builder()
                .id(7L)
                .title("Updated Title")
                .genres(Set.of("DRAMA"))
                .duration(100)
                .language("EN")
                .rating("PG")
                .releaseDate(LocalDate.parse("2020-01-01"))
                .build();

        when(movieService.updateMovie(eq(7L), any(UpdateMovieRequest.class))).thenReturn(updated);

        String json = """
                {
                  "title": "Updated Title",
                  "genres": ["DRAMA"],
                  "duration": 100
                }
                """;

        mockMvc.perform(put("/admin/movies/{id}", 7)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.duration").value(100));

        verify(movieService).updateMovie(eq(7L), any(UpdateMovieRequest.class));
        verifyNoMoreInteractions(movieService);
    }

    @Test
    void deleteMovie_callsService_andReturns200() throws Exception {
        doNothing().when(movieService).deleteMovie(9L);

        mockMvc.perform(delete("/admin/movies/{id}", 9))
                .andExpect(status().isOk());

        verify(movieService).deleteMovie(9L);
        verifyNoMoreInteractions(movieService);
    }
}
