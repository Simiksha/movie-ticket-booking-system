package com.moviebooking.movie_ticket_booking.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moviebooking.movie_ticket_booking.dto.ShowResponse;
import com.moviebooking.movie_ticket_booking.dto.ShowSeatResponse;
import com.moviebooking.movie_ticket_booking.show.ShowService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/shows")
@RequiredArgsConstructor
public class ShowController {

    private final ShowService showService;

    @GetMapping("/{showId}/seats")
    public List<ShowSeatResponse> getSeats(@PathVariable Long showId) {
        return showService.getSeatsForShow(showId);
    }

    @GetMapping
    public Page<ShowResponse> getShows(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,

            @RequestParam(required = false) Long theaterId,

            @RequestParam(required = false) Long movieId,

            @RequestParam(required = false) String city,

            Pageable pageable) {

        return showService.getShows(date, theaterId, movieId, city, pageable);
    }
}
