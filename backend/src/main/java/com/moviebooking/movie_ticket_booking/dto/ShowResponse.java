package com.moviebooking.movie_ticket_booking.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;

@Builder
public record ShowResponse(
        Long id,
        String movieTitle,
        String screenName,
        String theaterName,
        LocalDateTime showTime,
        BigDecimal price) {

}
