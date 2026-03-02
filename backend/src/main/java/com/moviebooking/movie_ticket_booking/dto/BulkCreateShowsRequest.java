package com.moviebooking.movie_ticket_booking.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BulkCreateShowsRequest(
    @NotNull Long movieId,
        @NotNull Long screenId,

        LocalDate startDate,

        Integer days,

        LocalDate endDate,

        @NotEmpty List<LocalTime> times,

        @NotNull @Positive BigDecimal price
) {
} 
