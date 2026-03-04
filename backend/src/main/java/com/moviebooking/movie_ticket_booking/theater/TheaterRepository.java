package com.moviebooking.movie_ticket_booking.theater;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


@Repository
public interface TheaterRepository extends  JpaRepository<Theater, Long> {
    
    Optional<Theater> findByNameAndCity(String name, String city);

    @Query("""
        select distinct t.city
        from Theater t
        where t.active = true and t.city is not null
        order by t.city
    """)
    List<String> findDistinctCities();
}
