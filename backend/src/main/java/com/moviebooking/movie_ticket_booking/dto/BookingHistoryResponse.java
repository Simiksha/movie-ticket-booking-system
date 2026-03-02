package com.moviebooking.movie_ticket_booking.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingHistoryResponse {
    private Long bookingId;
    private String movieTitle;
    private String theaterName;
    private String screenName;
    private LocalDateTime showTime;
    private List<String> seats;
    private String bookingStatus;
    private String paymentStatus;
    private BigDecimal totalAmount;
    private LocalDateTime bookedAt;
}
