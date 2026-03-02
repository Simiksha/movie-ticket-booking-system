package com.moviebooking.movie_ticket_booking.show;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moviebooking.movie_ticket_booking.dto.BulkCreateShowsRequest;
import com.moviebooking.movie_ticket_booking.dto.CreateShowRequest;
import com.moviebooking.movie_ticket_booking.dto.ShowResponse;
import com.moviebooking.movie_ticket_booking.dto.ShowSeatResponse;
import com.moviebooking.movie_ticket_booking.dto.UpdateShowRequest;
import com.moviebooking.movie_ticket_booking.exception.BadRequestException;
import com.moviebooking.movie_ticket_booking.exception.ResourceNotFoundException;
import com.moviebooking.movie_ticket_booking.model.Movie;
import com.moviebooking.movie_ticket_booking.model.Show;
import com.moviebooking.movie_ticket_booking.repository.MovieRepository;
import com.moviebooking.movie_ticket_booking.repository.ShowRepository;
import com.moviebooking.movie_ticket_booking.theater.Screen;
import com.moviebooking.movie_ticket_booking.theater.ScreenRepository;
import com.moviebooking.movie_ticket_booking.theater.Seat;
import com.moviebooking.movie_ticket_booking.theater.SeatRepository;
import com.moviebooking.movie_ticket_booking.theater.ShowSeat;
import com.moviebooking.movie_ticket_booking.theater.ShowSeatRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ShowServiceImpl implements ShowService {

        private final ShowRepository showRepository;
        private final ShowSeatRepository showSeatRepository;
        private final SeatRepository seatRepository;
        private final ScreenRepository screenRepository;
        private final MovieRepository movieRepository;

        // ================= CREATE SHOW =================

        @Override
        public Long createShow(CreateShowRequest request) {

                Movie movie = movieRepository.findById(request.movieId())
                                .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));

                Screen screen = screenRepository.findById(request.screenId())
                                .orElseThrow(() -> new ResourceNotFoundException("Screen not found"));

                Show show = Show.builder()
                                .movie(movie)
                                .screen(screen)
                                .showTime(request.showTime())
                                .price(BigDecimal.valueOf(request.price()))
                                .build();

                Show savedShow = showRepository.save(show);

                // Auto-generate ShowSeats
                List<Seat> seats = seatRepository.findByScreenId(request.screenId());

                List<ShowSeat> showSeats = seats.stream()
                                .map(seat -> ShowSeat.builder()
                                                .show(savedShow)
                                                .seat(seat)
                                                .price(BigDecimal.valueOf(request.price()))
                                                .booked(false)
                                                .build())
                                .toList();

                showSeatRepository.saveAll(showSeats);

                return savedShow.getId();
        }

        // ================= UPDATE SHOW =================

        @Override
        public void updateShow(Long showId, UpdateShowRequest request) {

                Show show = showRepository.findById(showId)
                                .orElseThrow(() -> new ResourceNotFoundException("Show not found"));

                show.setShowTime(request.showTime());
                show.setPrice(request.price());

                // Update price for all seats
                List<ShowSeat> showSeats = showSeatRepository.findByShowId(showId);
                showSeats.forEach(ss -> ss.setPrice(request.price()));
        }

        // ================= DELETE SHOW =================

        @Override
        public void deleteShow(Long showId) {

                Show show = showRepository.findById(showId)
                                .orElseThrow(() -> new ResourceNotFoundException("Show not found"));

                showSeatRepository.deleteByShowId(showId);
                showRepository.delete(show);
        }

        // ================= GET ALL SHOWS =================

        @Override
        public List<ShowResponse> getAllShows() {

                return showRepository.findAll()
                                .stream()
                                .map(show -> new ShowResponse(
                                                show.getId(),
                                                show.getMovie().getTitle(),
                                                show.getScreen().getName(),
                                                show.getScreen().getTheater().getName(),
                                                show.getShowTime(),
                                                show.getPrice()))
                                .toList();
        }

        // ================= FILTER SHOWS =================

        @Override
        public Page<ShowResponse> getShows(
                        LocalDate date,
                        Long theaterId,
                        Long movieId,
                        Pageable pageable) {

                Specification<Show> spec = ShowSpecification.filterShows(date, theaterId, movieId);

                return showRepository.findAll(spec, pageable)
                                .map(show -> new ShowResponse(
                                                show.getId(),
                                                show.getMovie().getTitle(),
                                                show.getScreen().getName(),
                                                show.getScreen().getTheater().getName(),
                                                show.getShowTime(),
                                                show.getPrice()));
        }

        // ================= GET SEATS =================

        @Override
        public List<ShowSeatResponse> getSeatsForShow(Long showId) {

                List<ShowSeat> showSeats = showSeatRepository.findByShowId(showId);

                return showSeats.stream()
                                .map(ss -> new ShowSeatResponse(
                                                ss.getId(),
                                                ss.getSeat().getId(),
                                                ss.getSeat().getSeatNumber(),
                                                ss.getSeat().getSeatType().name(),
                                                ss.isBooked(),
                                                ss.getPrice()))
                                .toList();
        }

        @Override
        public int bulkCreateShows(BulkCreateShowsRequest req) {

                LocalDate today = LocalDate.now();
                LocalDate start = (req.startDate() == null) ? today : req.startDate();

                if (start.isBefore(today)) {
                        throw new BadRequestException("Start date cannot be in the past");
                }

                int defaultDays = 14;

                LocalDate end;
                if (req.endDate() != null) {
                        end = req.endDate();
                } else {
                        int days = (req.days() == null) ? defaultDays : req.days();
                        if (days < 1)
                                throw new BadRequestException("days must be >= 1");
                        end = start.plusDays(days - 1);
                }

                if (end.isBefore(start)) {
                        throw new BadRequestException("endDate cannot be before startDate");
                }

                Movie movie = movieRepository.findById(req.movieId())
                                .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));

                Screen screen = screenRepository.findById(req.screenId())
                                .orElseThrow(() -> new ResourceNotFoundException("Screen not found"));

                List<Seat> seats = seatRepository.findByScreenId(req.screenId());
                if (seats.isEmpty())
                        throw new BadRequestException("No seats found for this screen");

                int createdCount = 0;

                for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                        for (LocalTime t : req.times()) {

                                LocalDateTime showTime = d.atTime(t);

                                // skip duplicates 
                                if (showRepository.existsByScreenIdAndShowTime(screen.getId(), showTime)) {
                                        continue;
                                }

                                Show show = Show.builder()
                                                .movie(movie)
                                                .screen(screen)
                                                .showTime(showTime)
                                                .price(req.price()) 
                                                .build();

                                Show savedShow = showRepository.save(show);

                                // auto-generate show_seats
                                List<ShowSeat> showSeats = seats.stream()
                                                .map(seat -> ShowSeat.builder()
                                                                .show(savedShow)
                                                                .seat(seat)
                                                                .price(req.price())
                                                                .booked(false)
                                                                .build())
                                                .toList();

                                showSeatRepository.saveAll(showSeats);

                                createdCount++;
                        }
                }

                return createdCount;
        }
}
