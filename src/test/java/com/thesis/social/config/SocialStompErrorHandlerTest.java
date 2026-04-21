package com.thesis.social.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesis.social.common.exception.ForbiddenException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

class SocialStompErrorHandlerTest {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldCreateValidationErrorFrameWithPayload() throws Exception {
        SocialStompErrorHandler handler = new SocialStompErrorHandler(objectMapper);
        Message<byte[]> clientMessage = buildClientMessage();

        Message<byte[]> response = handler.handleClientMessageProcessingError(
            clientMessage,
            new IllegalArgumentException("channelId is required")
        );

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(response);
        assertEquals(StompCommand.ERROR, accessor.getCommand());
        assertEquals("r-1", accessor.getReceiptId());

        Map<String, Object> payload = objectMapper.readValue(response.getPayload(), MAP_TYPE);
        assertEquals(400, payload.get("status"));
        assertEquals("validation_error", payload.get("error"));
        assertEquals("/app/chat.send", payload.get("path"));
    }

    @Test
    void shouldMapForbiddenExceptionTo403Payload() throws Exception {
        SocialStompErrorHandler handler = new SocialStompErrorHandler(objectMapper);

        Message<byte[]> response = handler.handleClientMessageProcessingError(
            buildClientMessage(),
            new ForbiddenException("Not a participant")
        );

        Map<String, Object> payload = objectMapper.readValue(response.getPayload(), MAP_TYPE);
        assertEquals(403, payload.get("status"));
        assertEquals("forbidden", payload.get("error"));
        assertEquals("Not a participant", payload.get("message"));
    }

    @Test
    void shouldReturn500ForUnhandledException() throws Exception {
        SocialStompErrorHandler handler = new SocialStompErrorHandler(objectMapper);

        Message<byte[]> response = handler.handleClientMessageProcessingError(
            buildClientMessage(),
            new RuntimeException("boom")
        );

        Map<String, Object> payload = objectMapper.readValue(response.getPayload(), MAP_TYPE);
        assertEquals(500, payload.get("status"));
        assertEquals("internal_error", payload.get("error"));
        assertEquals("Unexpected websocket error", payload.get("message"));
    }

    private Message<byte[]> buildClientMessage() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/chat.send");
        accessor.setReceipt("r-1");
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
