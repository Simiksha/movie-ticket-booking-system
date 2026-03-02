package com.moviebooking.movie_ticket_booking.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.moviebooking.movie_ticket_booking.theater.Screen;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "shows",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"screen_id", "show_time"})
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Show {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Movie being played
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    // Screen where it runs
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screen_id", nullable = false)
    private Screen screen;

    // Date & time of show
    @Column(name = "show_time", nullable = false)
    private LocalDateTime showTime;

    // Ticket price
    @Column(nullable = false)
    private BigDecimal price;

    // Relationship with bookings
    @Builder.Default
    @OneToMany(mappedBy = "show", cascade = CascadeType.ALL)
    private List<Booking> bookings = new ArrayList<>();
}
