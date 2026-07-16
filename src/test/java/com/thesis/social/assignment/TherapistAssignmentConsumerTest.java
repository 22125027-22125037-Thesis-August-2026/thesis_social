package com.thesis.social.assignment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

@ExtendWith(MockitoExtension.class)
class TherapistAssignmentConsumerTest {

    private static final long DELIVERY_TAG = 42L;

    @Mock
    private TherapistRelationshipService therapistRelationshipService;
    @Mock
    private Channel channel;

    private TherapistAssignmentConsumer consumer;

    private final UUID therapistId = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        consumer = new TherapistAssignmentConsumer(new ObjectMapper(), therapistRelationshipService);
    }

    private Message message(String body) {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(DELIVERY_TAG);
        return new Message(body.getBytes(StandardCharsets.UTF_8), properties);
    }

    private String activeEventBody() {
        return """
            {"eventId":"%s","occurredAt":"2026-07-16T10:00:00Z",
             "therapistProfileId":"%s","patientProfileId":"%s",
             "status":"ACTIVE","assignedAt":"2026-07-16T10:00:00Z"}
            """.formatted(UUID.randomUUID(), therapistId, patientId);
    }

    @Test
    void activeEventShouldLinkAndAck() throws IOException {
        consumer.onAssignmentChanged(message(activeEventBody()), channel);

        verify(therapistRelationshipService).linkTherapistAndPatient(therapistId, patientId);
        verify(channel).basicAck(DELIVERY_TAG, false);
    }

    @Test
    void inactiveEventShouldAckWithoutLinking() throws IOException {
        String body = activeEventBody().replace("\"ACTIVE\"", "\"INACTIVE\"");

        consumer.onAssignmentChanged(message(body), channel);

        verify(therapistRelationshipService, never()).linkTherapistAndPatient(any(), any());
        verify(channel).basicAck(DELIVERY_TAG, false);
    }

    @Test
    void malformedEventShouldDeadLetter() throws IOException {
        consumer.onAssignmentChanged(message("{\"status\":\"ACTIVE\"}"), channel);

        verify(therapistRelationshipService, never()).linkTherapistAndPatient(any(), any());
        verify(channel).basicNack(DELIVERY_TAG, false, false);
    }

    @Test
    void processingFailureShouldDeadLetter() throws IOException {
        doThrow(new RuntimeException("db down"))
            .when(therapistRelationshipService).linkTherapistAndPatient(therapistId, patientId);

        consumer.onAssignmentChanged(message(activeEventBody()), channel);

        verify(channel).basicNack(DELIVERY_TAG, false, false);
        verify(channel, never()).basicAck(anyLong(), eq(false));
    }
}
