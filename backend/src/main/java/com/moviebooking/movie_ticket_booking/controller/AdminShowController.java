package com.moviebooking.movie_ticket_booking.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moviebooking.movie_ticket_booking.dto.BulkCreateShowsRequest;
import com.moviebooking.movie_ticket_booking.dto.CreateShowRequest;
import com.moviebooking.movie_ticket_booking.dto.ShowResponse;
import com.moviebooking.movie_ticket_booking.dto.UpdateShowRequest;
import com.moviebooking.movie_ticket_booking.show.ShowService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/shows")
@RequiredArgsConstructor
public class AdminShowController {

    private final ShowService showService;

    @PostMapping
    public ResponseEntity<Long> createShow(
            @Valid @RequestBody CreateShowRequest request) {

        return ResponseEntity.ok(showService.createShow(request));
    }

    @PutMapping("/{id}")
    public void updateShow(
            @PathVariable Long id,
            @Valid @RequestBody UpdateShowRequest request) {

        showService.updateShow(id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteShow(@PathVariable Long id) {
        showService.deleteShow(id);
    }

    @GetMapping
    public List<ShowResponse> getAllShows() {
        return showService.getAllShows();
    }

    @PostMapping("/bulk")
    public ResponseEntity<Integer> bulkCreateShows(@Valid @RequestBody BulkCreateShowsRequest request) {
        int created = showService.bulkCreateShows(request);
        return ResponseEntity.ok(created); 
    }
}
