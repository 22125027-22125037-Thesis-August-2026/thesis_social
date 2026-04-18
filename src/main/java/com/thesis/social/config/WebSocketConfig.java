package com.thesis.social.config;

import com.thesis.social.security.StompJwtChannelInterceptor;
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

    public WebSocketConfig(SocialProperties properties, StompJwtChannelInterceptor stompJwtChannelInterceptor) {
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
}
