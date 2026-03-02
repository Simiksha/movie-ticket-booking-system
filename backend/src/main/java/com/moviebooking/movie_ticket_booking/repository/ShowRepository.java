package com.moviebooking.movie_ticket_booking.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.moviebooking.movie_ticket_booking.model.Show;

@Repository
public interface ShowRepository extends JpaRepository<Show, Long>, JpaSpecificationExecutor<Show>  {
    boolean existsByScreenIdAndShowTime(Long screenId, LocalDateTime showTime);
}
