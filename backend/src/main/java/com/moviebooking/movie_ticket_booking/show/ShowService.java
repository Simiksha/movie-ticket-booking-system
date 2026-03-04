package com.moviebooking.movie_ticket_booking.show;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moviebooking.movie_ticket_booking.dto.BulkCreateShowsRequest;
import com.moviebooking.movie_ticket_booking.dto.CreateShowRequest;
import com.moviebooking.movie_ticket_booking.dto.ShowResponse;
import com.moviebooking.movie_ticket_booking.dto.ShowSeatResponse;
import com.moviebooking.movie_ticket_booking.dto.UpdateShowRequest;

public interface ShowService {
    Long createShow(CreateShowRequest request);

    void updateShow(Long showId, UpdateShowRequest request);

    void deleteShow(Long showId);

    List<ShowResponse> getAllShows();

    Page<ShowResponse> getShows(LocalDate date, Long theaterId, Long movieId, String city, Pageable pageable);

    List<ShowSeatResponse> getSeatsForShow(Long showId);

    int bulkCreateShows(BulkCreateShowsRequest req);

}
