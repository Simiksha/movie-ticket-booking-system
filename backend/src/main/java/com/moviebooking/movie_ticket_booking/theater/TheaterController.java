package com.moviebooking.movie_ticket_booking.theater;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moviebooking.movie_ticket_booking.theater.dto.CreateScreenRequest;
import com.moviebooking.movie_ticket_booking.theater.dto.CreateTheaterRequest;
import com.moviebooking.movie_ticket_booking.theater.dto.ScreenResponse;
import com.moviebooking.movie_ticket_booking.theater.dto.TheaterResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/theaters")
@RequiredArgsConstructor
public class TheaterController {

    private final TheaterService theaterService;

    @GetMapping
    public List<TheaterResponse> getAllTheaters() {
        return theaterService.getAllTheaters();
    }

    @PostMapping
    public ResponseEntity<Long> createTheater(
            @Valid @RequestBody CreateTheaterRequest request) {
        return ResponseEntity.ok(theaterService.createTheater(request));
    }

    @PostMapping("/{theaterId}/screens")
    public ResponseEntity<Long> addScreen(
            @PathVariable Long theaterId,
            @Valid @RequestBody CreateScreenRequest request) {
        return ResponseEntity.ok(theaterService.addScreen(theaterId, request));
    }

    @GetMapping("/{theaterId}/screens")
    public List<ScreenResponse> getScreens(@PathVariable Long theaterId) {
        return theaterService.getScreens(theaterId);
    }

}
