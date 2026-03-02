package com.moviebooking.movie_ticket_booking.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateShowRequest(
        @NotNull LocalDateTime showTime,
        @NotNull @Positive BigDecimal price) 
        {}
