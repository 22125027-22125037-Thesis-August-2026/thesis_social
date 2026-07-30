package com.thesis.social.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;

class TherapistAssignmentMessagingConfigTest {

    private final TherapistAssignmentMessagingConfig config = new TherapistAssignmentMessagingConfig();

    /**
     * Regression: RabbitProperties leaves virtualHost null when spring.rabbitmq.virtual-host is
     * unset, and forwarding that null wiped the driver's "/" default. Every AMQP connection then
     * failed with "Invalid configuration: 'virtualHost' must be non-null", which surfaced as a
     * 500 on every endpoint that publishes a domain event — friend requests included.
     */
    @Test
    void shouldKeepDefaultVirtualHostWhenNoneConfigured() {
        CachingConnectionFactory connectionFactory = config.rabbitConnectionFactory(new RabbitProperties());

        assertEquals("/", connectionFactory.getVirtualHost());
    }

    @Test
    void shouldApplyConfiguredVirtualHost() {
        RabbitProperties properties = new RabbitProperties();
        properties.setVirtualHost("social");

        CachingConnectionFactory connectionFactory = config.rabbitConnectionFactory(properties);

        assertEquals("social", connectionFactory.getVirtualHost());
    }
}
