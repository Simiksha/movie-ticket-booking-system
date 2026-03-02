package com.moviebooking.movie_ticket_booking.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ShowSeatResponse {
    
    private Long showSeatId;
    private Long seatId;
    private String seatNumber;
    private String seatType;
    private boolean booked;
    private BigDecimal price;

}
