package com.moviebooking.movie_ticket_booking.theater;

import java.util.List;

import com.moviebooking.movie_ticket_booking.theater.dto.CreateScreenRequest;
import com.moviebooking.movie_ticket_booking.theater.dto.CreateTheaterRequest;
import com.moviebooking.movie_ticket_booking.theater.dto.ScreenResponse;
import com.moviebooking.movie_ticket_booking.theater.dto.TheaterResponse;

public interface TheaterService {
    Long createTheater(CreateTheaterRequest request);

    Long addScreen(Long theaterId, CreateScreenRequest request);

    List<TheaterResponse> getAllTheaters();

    List<ScreenResponse> getScreens(Long theaterId);
}
