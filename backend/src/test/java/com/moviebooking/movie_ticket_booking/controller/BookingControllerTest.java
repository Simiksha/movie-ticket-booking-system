package com.moviebooking.movie_ticket_booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moviebooking.movie_ticket_booking.dto.BookingRequest;
import com.moviebooking.movie_ticket_booking.service.BookingService;
import com.moviebooking.movie_ticket_booking.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void shouldCreateBookingSuccessfully() throws Exception {
        BookingRequest request = new BookingRequest();
        when(bookingService.createBooking(anyString(), any(BookingRequest.class)))
                .thenReturn(101L);

        mockMvc.perform(post("/bookings")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("101"));
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void shouldGetMyBookings() throws Exception {
        when(bookingService.getMyBookings(anyString()))
                .thenReturn(java.util.List.of());

        mockMvc.perform(get("/bookings/my-bookings"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void shouldCancelBooking() throws Exception {
        doNothing().when(paymentService).cancelAndRefund(101L, "test@example.com");

        mockMvc.perform(delete("/bookings/101")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Booking cancelled successfully"));
    }

    @Test
    void shouldReturnForbiddenWhenUserNotLoggedIn() throws Exception {
        mockMvc.perform(get("/bookings/my-bookings"))
                .andExpect(status().isForbidden());
    }
}