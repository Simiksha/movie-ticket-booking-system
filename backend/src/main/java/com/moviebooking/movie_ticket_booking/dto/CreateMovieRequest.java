package com.moviebooking.movie_ticket_booking.dto;

import java.time.LocalDate;
import java.util.Set;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMovieRequest {
    @NotBlank
    private String title;

    private String description;

    @NotEmpty
    private Set<String> genres; 

    @NotNull
    @Min(1)
    private Integer duration;

    @NotBlank
    private String language;

    @NotBlank
    private String rating;

    @NotNull
    private LocalDate releaseDate;

    private String posterUrl;
}
