package com.moviebooking.movie_ticket_booking.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
public class EmailServiceImplTest {
    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailServiceImpl emailService;

    @Test
    void sendBookingConfirmation_buildsMessageAndSends() {
        String toEmail = "user@test.com";
        String subject = "Booking Confirmed 🎬";
        String body = "Your booking is confirmed.";

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailService.sendBookingConfirmation(toEmail, subject, body);

        verify(mailSender, times(1)).send(captor.capture());

        SimpleMailMessage msg = captor.getValue();
        assertNotNull(msg);

        assertArrayEquals(new String[]{toEmail}, msg.getTo());
        assertEquals(subject, msg.getSubject());
        assertEquals(body, msg.getText());
    }

    @Test
    void sendBookingConfirmation_whenMailSenderThrows_propagatesException() {
        doThrow(new RuntimeException("SMTP error"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThrows(RuntimeException.class, () ->
                emailService.sendBookingConfirmation("a@b.com", "sub", "body")
        );

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}
