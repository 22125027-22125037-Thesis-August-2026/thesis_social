package com.thesis.social.event;

import com.thesis.social.config.SocialProperties;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class RabbitDomainEventPublisher implements DomainEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final SocialProperties properties;

    public RabbitDomainEventPublisher(RabbitTemplate rabbitTemplate, SocialProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    @Override
    public void publish(String eventType, Map<String, Object> payload) {
        Map<String, Object> eventEnvelope = new HashMap<>();
        eventEnvelope.put("eventId", UUID.randomUUID());
        eventEnvelope.put("eventType", eventType);
        eventEnvelope.put("occurredAt", OffsetDateTime.now());
        eventEnvelope.put("payload", payload);

        String routingKey = properties.getEvent().getRoutingPrefix() + "." + eventType;
        rabbitTemplate.convertAndSend(properties.getEvent().getExchange(), routingKey, eventEnvelope);
    }
}
