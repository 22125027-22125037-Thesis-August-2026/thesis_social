package com.thesis.social.security;

import com.thesis.social.common.exception.UnauthorizedException;
import java.security.Principal;
import java.util.List;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
public class StompJwtChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenService jwtTokenService;

    public StompJwtChannelInterceptor(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractBearer(accessor.getNativeHeader("Authorization"));
            AuthenticatedProfile profile = jwtTokenService.validateAndExtract(token);
            accessor.setUser(profile);
            return message;
        }

        Principal user = accessor.getUser();
        if (user == null && !StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            throw new UnauthorizedException("Unauthenticated websocket frame");
        }

        return message;
    }

    private String extractBearer(List<String> authHeader) {
        if (authHeader == null || authHeader.isEmpty()) {
            throw new UnauthorizedException("Missing STOMP authorization header");
        }
        String value = authHeader.get(0);
        if (!value.startsWith("Bearer ")) {
            throw new UnauthorizedException("Invalid STOMP authorization format");
        }
        return value.substring(7);
    }
}
