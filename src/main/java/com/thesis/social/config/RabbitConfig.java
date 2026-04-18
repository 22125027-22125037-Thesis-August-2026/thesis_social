package com.thesis.social.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    TopicExchange socialEventsExchange(SocialProperties socialProperties) {
        return new TopicExchange(socialProperties.getEvent().getExchange(), true, false);
    }
}
