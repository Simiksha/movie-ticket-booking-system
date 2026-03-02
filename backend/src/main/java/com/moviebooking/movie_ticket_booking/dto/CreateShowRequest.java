package com.moviebooking.movie_ticket_booking.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateShowRequest(

        @NotNull Long movieId,
        @NotNull Long screenId,
        @NotNull LocalDateTime showTime,
        @NotNull @Positive Double price
) {}
