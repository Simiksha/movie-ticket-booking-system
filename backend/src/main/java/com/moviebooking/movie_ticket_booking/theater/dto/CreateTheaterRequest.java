package com.moviebooking.movie_ticket_booking.theater.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateTheaterRequest(
        @NotBlank String name,
        @NotBlank String city,
        @NotBlank String address) {

}
