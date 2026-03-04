package com.moviebooking.movie_ticket_booking.theater;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moviebooking.movie_ticket_booking.theater.dto.ScreenResponse;
import com.moviebooking.movie_ticket_booking.theater.dto.TheaterResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/theaters")
@RequiredArgsConstructor
public class PublicTheaterController {

    private final TheaterService theaterService;
    private final TheaterRepository theaterRepository;

    @GetMapping
    public List<TheaterResponse> getAllTheaters() {
        return theaterService.getAllTheaters();
    }

    @GetMapping("/cities")
    public List<String> getCities() {
        return theaterRepository.findDistinctCities();
    }

    @GetMapping("/{theaterId}/screens")
    public List<ScreenResponse> getScreens(@PathVariable Long theaterId) {
        return theaterService.getScreens(theaterId);
    }
}
