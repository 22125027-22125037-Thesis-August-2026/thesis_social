package com.thesis.social.assignment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.thesis.social.config.TherapistAssignmentMessagingConfig;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes therapist-api assignment-change events and creates the friendship + direct chat
 * channel for every newly ACTIVE therapist&lt;-&gt;patient match.
 *
 * <p>INACTIVE events are acknowledged without action: when a patient is re-matched, the chat
 * and relationship with the previous therapist are deliberately kept.
 *
 * <p>Manual ack with a single consumer (see {@link TherapistAssignmentMessagingConfig}).
 * Failure handling avoids infinite nack loops:
 * <ul>
 *   <li>Malformed / unparseable messages are dead-lettered immediately.</li>
 *   <li>Transient processing errors are also dead-lettered (requeue=false) rather than
 *       requeued forever; the linking is idempotent, so a later re-match heals the gap.</li>
 * </ul>
 */
@Component
public class TherapistAssignmentConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TherapistAssignmentConsumer.class);

    private static final String STATUS_ACTIVE = "ACTIVE";

    private final ObjectMapper objectMapper;
    private final TherapistRelationshipService therapistRelationshipService;

    public TherapistAssignmentConsumer(ObjectMapper objectMapper,
                                       TherapistRelationshipService therapistRelationshipService) {
        this.objectMapper = objectMapper;
        this.therapistRelationshipService = therapistRelationshipService;
    }

    @RabbitListener(
            queues = TherapistAssignmentMessagingConfig.ASSIGNMENT_QUEUE,
            containerFactory = TherapistAssignmentMessagingConfig.MANUAL_ACK_FACTORY)
    public void onAssignmentChanged(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        TherapistAssignmentEvent event;
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            JsonNode node = objectMapper.readTree(body);
            event = TherapistAssignmentEvent.fromJson(node);
            if (event == null || !event.isValid()) {
                throw new IllegalArgumentException("missing required fields: " + body);
            }
        } catch (Exception parseError) {
            LOGGER.error("Malformed assignment event, dead-lettering: {}", parseError.getMessage());
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        if (!STATUS_ACTIVE.equals(event.status().trim().toUpperCase(Locale.ROOT))) {
            // Deactivations keep the existing chat/relationship by design.
            channel.basicAck(deliveryTag, false);
            return;
        }

        try {
            therapistRelationshipService.linkTherapistAndPatient(
                    event.therapistProfileId(),
                    event.patientProfileId());
            channel.basicAck(deliveryTag, false);
        } catch (Exception processingError) {
            LOGGER.error("Failed to link therapist={} patient={}, dead-lettering",
                    event.therapistProfileId(), event.patientProfileId(), processingError);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
