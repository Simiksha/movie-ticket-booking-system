package com.moviebooking.movie_ticket_booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.moviebooking.movie_ticket_booking.dto.CreateMovieRequest;
import com.moviebooking.movie_ticket_booking.dto.MovieResponse;
import com.moviebooking.movie_ticket_booking.dto.UpdateMovieRequest;
import com.moviebooking.movie_ticket_booking.exception.ResourceNotFoundException;
import com.moviebooking.movie_ticket_booking.model.Genre;
import com.moviebooking.movie_ticket_booking.model.Movie;
import com.moviebooking.movie_ticket_booking.repository.GenreRepository;
import com.moviebooking.movie_ticket_booking.repository.MovieRepository;

@ExtendWith(MockitoExtension.class)
class MovieServiceImplTest {

    @Mock MovieRepository movieRepository;
    @Mock GenreRepository genreRepository;

    @InjectMocks MovieServiceImpl movieService;

    @Test
    void createMovie_createsMissingGenres_savesMovie_andReturnsResponse() {
        // arrange
        CreateMovieRequest req = mock(CreateMovieRequest.class);
        when(req.getTitle()).thenReturn("Inception");
        when(req.getDescription()).thenReturn("Dreams");
        when(req.getGenres()).thenReturn(Set.of(" sci-fi ", "action"));
        when(req.getDuration()).thenReturn(148);
        when(req.getLanguage()).thenReturn("EN");
        when(req.getRating()).thenReturn("8.8");
        when(req.getReleaseDate()).thenReturn(LocalDate.of(2010, 7, 16));
        when(req.getPosterUrl()).thenReturn("http://poster");

        // sci-fi exists, action does not
        when(genreRepository.findByName("SCI-FI"))
                .thenReturn(Optional.of(Genre.builder().name("SCI-FI").build()));
        when(genreRepository.findByName("ACTION"))
                .thenReturn(Optional.empty());
        when(genreRepository.save(any(Genre.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // emulate id assignment on save
        when(movieRepository.save(any(Movie.class))).thenAnswer(inv -> {
            Movie m = inv.getArgument(0);
            if (m.getId() == null) m.setId(1L);
            return m;
        });

        // act
        MovieResponse res = movieService.createMovie(req);

        // assert
        assertThat(res.getId()).isEqualTo(1L);
        assertThat(res.getTitle()).isEqualTo("Inception");
        assertThat(res.getGenres()).containsExactlyInAnyOrder("SCI-FI", "ACTION");

        ArgumentCaptor<Movie> movieCaptor = ArgumentCaptor.forClass(Movie.class);
        verify(movieRepository).save(movieCaptor.capture());

        Movie savedMovie = movieCaptor.getValue();
        assertThat(savedMovie.isActive()).isTrue();
        assertThat(savedMovie.getGenres()).hasSize(2);

        verify(genreRepository).findByName("SCI-FI");
        verify(genreRepository).findByName("ACTION");
        verify(genreRepository, times(1)).save(Mockito.argThat(g -> "ACTION".equals(g.getName())));
    }

    @Test
    void updateMovie_notFound_throwsResourceNotFound() {
        when(movieRepository.findById(99L)).thenReturn(Optional.empty());

        UpdateMovieRequest req = mock(UpdateMovieRequest.class);

        assertThatThrownBy(() -> movieService.updateMovie(99L, req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Movie not found with id 99");

        verify(movieRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateMovie_updatesOnlyProvidedFields_andReturnsResponse() {
        // arrange existing movie
        Movie existing = Movie.builder()
                .title("Old")
                .description("Old desc")
                .genres(new HashSet<>(Set.of(Genre.builder().name("DRAMA").build())))
                .duration(100)
                .language("EN")
                .rating("7.0")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .posterUrl("old")
                .active(true)
                .build();
        existing.setId(5L);

        when(movieRepository.findById(5L)).thenReturn(Optional.of(existing));

        UpdateMovieRequest req = mock(UpdateMovieRequest.class);
        when(req.getTitle()).thenReturn("New Title");
        when(req.getDescription()).thenReturn(null); // unchanged
        when(req.getGenres()).thenReturn(Set.of(" action ")); // overwrite
        when(req.getDuration()).thenReturn(null); // unchanged
        when(req.getLanguage()).thenReturn("TA");
        when(req.getRating()).thenReturn("9.1");
        when(req.getReleaseDate()).thenReturn(null); // unchanged
        when(req.getPosterUrl()).thenReturn("newPoster");
        when(req.getActive()).thenReturn(false);

        when(genreRepository.findByName("ACTION")).thenReturn(Optional.empty());
        when(genreRepository.save(any(Genre.class))).thenAnswer(inv -> inv.getArgument(0));

        when(movieRepository.saveAndFlush(any(Movie.class))).thenAnswer(inv -> inv.getArgument(0));

        // act
        MovieResponse res = movieService.updateMovie(5L, req);

        // assert updated fields
        assertThat(existing.getTitle()).isEqualTo("New Title");
        assertThat(existing.getDescription()).isEqualTo("Old desc");
        assertThat(existing.getLanguage()).isEqualTo("TA");
        assertThat(existing.getRating()).isEqualTo("9.1");
        assertThat(existing.getPosterUrl()).isEqualTo("newPoster");
        assertThat(existing.isActive()).isFalse();
        assertThat(existing.getGenres()).extracting(Genre::getName).containsExactly("ACTION");

        // response should reflect updated entity
        assertThat(res.getId()).isEqualTo(5L);
        assertThat(res.getTitle()).isEqualTo("New Title");
        assertThat(res.getGenres()).containsExactly("ACTION");

        verify(movieRepository).saveAndFlush(existing);
    }

    @Test
    void deleteMovie_marksInactive_doesNotDeleteRow() {
        Movie existing = Movie.builder().active(true).build();
        existing.setId(7L);

        when(movieRepository.findById(7L)).thenReturn(Optional.of(existing));

        movieService.deleteMovie(7L);

        assertThat(existing.isActive()).isFalse();
        verify(movieRepository, never()).delete(any());
        verify(movieRepository, never()).save(any()); // relies on @Transactional dirty checking
    }

    @Test
    void getAllMovies_mapsOnlyActiveMovies() {
        Movie m = Movie.builder()
                .title("A")
                .genres(Set.of(Genre.builder().name("ACTION").build()))
                .duration(120)
                .language("EN")
                .rating("8.0")
                .releaseDate(LocalDate.of(2020, 1, 1))
                .posterUrl("p")
                .build();
        m.setId(1L);

        Page<Movie> page = new PageImpl<>(List.of(m), PageRequest.of(0, 10), 1);

        when(movieRepository.findByActiveTrue(any(Pageable.class))).thenReturn(page);

        Page<MovieResponse> out = movieService.getAllMovies(PageRequest.of(0, 10));

        assertThat(out.getTotalElements()).isEqualTo(1);
        assertThat(out.getContent().get(0).getId()).isEqualTo(1L);
        assertThat(out.getContent().get(0).getGenres()).containsExactly("ACTION");
    }

    @Test
    void getMoviesByGenre_normalizesGenreToUppercase() {
        Page<Movie> empty = Page.empty(PageRequest.of(0, 10));
        when(movieRepository.findByGenres_NameAndActiveTrue(eq("SCI-FI"), any(Pageable.class)))
                .thenReturn(empty);

        Page<MovieResponse> out = movieService.getMoviesByGenre("sci-fi", PageRequest.of(0, 10));

        assertThat(out.getTotalElements()).isZero();
        verify(movieRepository).findByGenres_NameAndActiveTrue(eq("SCI-FI"), any(Pageable.class));
    }

}