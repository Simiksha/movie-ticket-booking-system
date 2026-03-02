package com.moviebooking.movie_ticket_booking.theater;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moviebooking.movie_ticket_booking.exception.BadRequestException;
import com.moviebooking.movie_ticket_booking.exception.ResourceNotFoundException;
import com.moviebooking.movie_ticket_booking.theater.dto.CreateScreenRequest;
import com.moviebooking.movie_ticket_booking.theater.dto.CreateTheaterRequest;
import com.moviebooking.movie_ticket_booking.theater.dto.ScreenResponse;
import com.moviebooking.movie_ticket_booking.theater.dto.TheaterResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TheaterServiceImpl implements TheaterService {

    private final TheaterRepository theaterRepository;
    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;

    @Transactional(readOnly = true)
    @Override
    public List<TheaterResponse> getAllTheaters() {
        return theaterRepository.findAll().stream()
                .map(t -> new TheaterResponse(t.getId(), t.getName(), t.getCity()))
                .toList();
    }

    @Override
    public Long createTheater(CreateTheaterRequest request) {

        theaterRepository.findByNameAndCity(request.name(), request.city())
                .ifPresent(t -> {
                    throw new BadRequestException("Theater already exists in this city");
                });

        Theater theater = Theater.builder()
                .name(request.name())
                .city(request.city())
                .address(request.address())
                .active(true)
                .build();

        return theaterRepository.save(theater).getId();
    }

    @Override
    @Transactional
    public Long addScreen(Long theaterId, CreateScreenRequest request) {

        Theater theater = theaterRepository.findById(theaterId)
                .orElseThrow(() -> new ResourceNotFoundException("Theater not found"));

        screenRepository.findByNameAndTheaterId(request.name(), theaterId)
                .ifPresent(s -> {
                    throw new BadRequestException("Screen already exists in this theater");
                });

        Screen screen = Screen.builder()
                .name(request.name())
                .totalSeats(request.totalSeats())
                .theater(theater)
                .build();

        Screen saved = screenRepository.save(screen);

        // Auto-generate seats
        int total = request.totalSeats();
        int seatsPerRow = 12;

        int regularLimit = (int) Math.floor(total * 0.70);
        int premiumLimit = (int) Math.floor(total * 0.90); 

        List<Seat> seats = new ArrayList<>(total);

        int created = 0;
        char row = 'A';

        while (created < total) {
            for (int col = 1; col <= seatsPerRow && created < total; col++) {

                SeatType type;
                if (created < regularLimit)
                    type = SeatType.REGULAR;
                else if (created < premiumLimit)
                    type = SeatType.PREMIUM;
                else
                    type = SeatType.RECLINER;

                seats.add(Seat.builder()
                        .screen(saved)
                        .seatNumber(row + String.valueOf(col))
                        .seatType(type)
                        .build());

                created++;
            }
            row++;
        }
        seatRepository.saveAll(seats);

        return saved.getId();
    }

    @Transactional(readOnly = true)
    @Override
    public List<ScreenResponse> getScreens(Long theaterId) {
        Theater theater = theaterRepository.findById(theaterId)
                .orElseThrow(() -> new ResourceNotFoundException("Theater not found"));

        return screenRepository.findByTheaterId(theater.getId()).stream()
                .map(s -> new ScreenResponse(s.getId(), s.getName(), s.getTotalSeats()))
                .toList();
    }
}
