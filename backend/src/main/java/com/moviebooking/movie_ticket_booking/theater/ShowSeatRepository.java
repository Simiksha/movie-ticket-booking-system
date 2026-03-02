package com.moviebooking.movie_ticket_booking.theater;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface ShowSeatRepository extends JpaRepository<ShowSeat, Long> {

    List<ShowSeat> findByShowId(Long showId);

    Optional<ShowSeat> findByShowIdAndSeatId(Long showId, Long seatId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ss FROM ShowSeat ss WHERE ss.id = :id")
    ShowSeat findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT ss FROM ShowSeat ss
            WHERE ss.show.id = :showId AND ss.seat.id IN :seatIds
            """)
    List<ShowSeat> findByShowIdAndSeatIdInForUpdate(@Param("showId") Long showId,
            @Param("seatIds") List<Long> seatIds);

    void deleteByShowId(Long showId);
}
