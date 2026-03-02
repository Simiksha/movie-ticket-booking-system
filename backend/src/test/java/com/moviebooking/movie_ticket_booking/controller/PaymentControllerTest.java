package com.moviebooking.movie_ticket_booking.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.moviebooking.movie_ticket_booking.config.SecurityConfig;
import com.moviebooking.movie_ticket_booking.security.JwtAuthenticationFilter;
import com.moviebooking.movie_ticket_booking.service.PaymentService;

@WebMvcTest(controllers = PaymentController.class, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class)
})
@AutoConfigureMockMvc(addFilters = false)
public class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void createOrder_returnsOk_andCallsService() throws Exception {
        Map<String, Object> fakeOrder = Map.of(
                "orderId", "order_123",
                "amount", 500);

        when(paymentService.createOrder(10L)).thenReturn(fakeOrder);

        mockMvc.perform(post("/payments/create-order/{bookingId}", 10L))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.orderId").value("order_123"))
                .andExpect(jsonPath("$.amount").value(500));

        verify(paymentService).createOrder(10L);
    }

    @Test
    void handleWebhook_returnsOk_andCallsService() throws Exception {
        String payload = "{\"event\":\"payment.captured\"}";
        String signature = "test-signature";

        doNothing().when(paymentService).handleWebhook(eq(payload), eq(signature));

        mockMvc.perform(post("/payments/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Razorpay-Signature", signature)
                .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().string("Webhook processed"));

        verify(paymentService).handleWebhook(payload, signature);
    }

    @Test
    void handleWebhook_missingSignatureHeader_returns400() throws Exception {
        String payload = "{\"event\":\"payment.captured\"}";

        mockMvc.perform(post("/payments/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_whenServiceThrows_returns500() throws Exception {
        when(paymentService.createOrder(99L)).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(post("/payments/create-order/{bookingId}", 99L))
                .andExpect(status().isInternalServerError());
    }
}
