package com.moviebooking.movie_ticket_booking.theater.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateScreenRequest(
        @NotBlank String name,

        @NotNull @Min(1) Integer totalSeats) {

}
