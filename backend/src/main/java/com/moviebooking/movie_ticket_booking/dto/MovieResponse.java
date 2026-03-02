package com.moviebooking.movie_ticket_booking.dto;

import java.time.LocalDate;
import java.util.Set;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MovieResponse {
    private Long id;
    private String title;
    private String description;
    private Set<String> genres;
    private Integer duration;
    private String language;
    private String rating;
    private LocalDate releaseDate;
    private String posterUrl;
}
