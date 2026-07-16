package com.thesis.social.config;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class RabbitConfig {

    /**
     * Admin for the social service's own broker. Defined explicitly (Boot's auto-configured
     * {@code amqpAdmin} backs off once any other {@link AmqpAdmin} bean exists — here the
     * shared-broker admin in {@link TherapistAssignmentMessagingConfig}); {@code @Primary}
     * keeps it the default for by-type injection. The injected connection factory resolves
     * to the primary (social-broker) one.
     */
    @Bean
    @Primary
    AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    TopicExchange socialEventsExchange(SocialProperties socialProperties, AmqpAdmin amqpAdmin) {
        TopicExchange exchange = new TopicExchange(socialProperties.getEvent().getExchange(), true, false);
        // Declare on the social broker only — not on the shared therapist broker's admin
        // (see TherapistAssignmentMessagingConfig).
        exchange.setAdminsThatShouldDeclare(amqpAdmin);
        return exchange;
    }
}
