package com.moviebooking.movie_ticket_booking.repository;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.moviebooking.movie_ticket_booking.model.Movie;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {

    Page<Movie> findByActiveTrue(Pageable pageable);

    Page<Movie> findByGenres_NameAndActiveTrue(String genre, Pageable pageable);

    @Query(value = """
                select distinct m
                from Movie m
                join m.shows s
                join s.screen sc
                join sc.theater t
                left join m.genres g
                where m.active = true
                  and (:city is null or lower(t.city) = :city)
                  and (:start is null or s.showTime between :start and :end)
                  and (:genre is null or lower(g.name) = :genre)
            """, countQuery = """
                select count(distinct m.id)
                from Movie m
                join m.shows s
                join s.screen sc
                join sc.theater t
                left join m.genres g
                where m.active = true
                  and (:city is null or lower(t.city) = :city)
                  and (:start is null or s.showTime between :start and :end)
                  and (:genre is null or lower(g.name) = :genre)
            """)
    Page<Movie> findAvailableMovies(
            @Param("genre") String genre,
            @Param("city") String city,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);
}
