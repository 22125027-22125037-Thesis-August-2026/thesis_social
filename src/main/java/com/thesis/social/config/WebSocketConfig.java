package com.thesis.social.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesis.social.security.StompJwtChannelInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final SocialProperties properties;
    private final StompJwtChannelInterceptor stompJwtChannelInterceptor;

    public WebSocketConfig(
        SocialProperties properties,
        StompJwtChannelInterceptor stompJwtChannelInterceptor
    ) {
        this.properties = properties;
        this.stompJwtChannelInterceptor = stompJwtChannelInterceptor;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(properties.getWebsocket().getEndpoint())
            .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableStompBrokerRelay("/queue", "/topic")
            // Pin the RabbitMQ virtual host. Without this Spring forwards whatever `host` header the
            // client put in its STOMP CONNECT frame, and stomp.js sets that to the broker URL's
            // hostname (umatter-apcs.duckdns.org). RabbitMQ only has "/", so it rejected every
            // CONNECT with "Virtual host '...' access denied" and no chat session could ever open.
            .setVirtualHost(properties.getBroker().getRelayVirtualHost())
            .setRelayHost(properties.getBroker().getRelayHost())
            .setRelayPort(properties.getBroker().getRelayPort())
            .setClientLogin(properties.getBroker().getRelayClientLogin())
            .setClientPasscode(properties.getBroker().getRelayClientPasscode())
            .setSystemLogin(properties.getBroker().getRelaySystemLogin())
            .setSystemPasscode(properties.getBroker().getRelaySystemPasscode());

        registry.setApplicationDestinationPrefixes(properties.getWebsocket().getAppDestinationPrefix());
        registry.setUserDestinationPrefix(properties.getWebsocket().getUserDestinationPrefix());
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompJwtChannelInterceptor);
    }

    @Bean
    SocialStompErrorHandler stompErrorHandler(ObjectMapper objectMapper) {
        return new SocialStompErrorHandler(objectMapper);
    }
}
