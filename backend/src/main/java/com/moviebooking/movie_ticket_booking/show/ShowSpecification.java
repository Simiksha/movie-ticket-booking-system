package com.moviebooking.movie_ticket_booking.show;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.data.jpa.domain.Specification;

import com.moviebooking.movie_ticket_booking.model.Show;

public class ShowSpecification {
        public static Specification<Show> filterShows(
                        LocalDate date,
                        Long theaterId,
                        Long movieId) {

                return (root, query, cb) -> {

                        var predicates = cb.conjunction();

                        if (date != null) {
                                LocalDateTime start = date.atStartOfDay();
                                LocalDateTime end = date.atTime(LocalTime.MAX);
                                predicates = cb.and(predicates, cb.between(root.get("showTime"), start, end));
                        }

                        if (theaterId != null) {
                                predicates = cb.and(predicates,
                                        cb.equal(
                                                root.get("screen")
                                                                .get("theater")
                                                                .get("id"),
                                                theaterId));
                        }

                        if (movieId != null) {
                                predicates = cb.and(predicates,
                                        cb.equal(
                                                root.get("movie")
                                                                .get("id"),
                                                movieId));
                        }

                        return predicates;
                };
        }
}
