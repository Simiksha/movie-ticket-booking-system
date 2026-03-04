package com.moviebooking.movie_ticket_booking.theater;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moviebooking.movie_ticket_booking.theater.dto.CreateScreenRequest;
import com.moviebooking.movie_ticket_booking.theater.dto.CreateTheaterRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/theaters")
@RequiredArgsConstructor
public class AdminTheaterController {

    private final TheaterService theaterService;

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

}
