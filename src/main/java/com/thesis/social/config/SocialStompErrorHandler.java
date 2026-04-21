package com.thesis.social.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesis.social.common.exception.ConflictException;
import com.thesis.social.common.exception.DomainException;
import com.thesis.social.common.exception.ForbiddenException;
import com.thesis.social.common.exception.NotFoundException;
import com.thesis.social.common.exception.UnauthorizedException;
import jakarta.validation.ConstraintViolationException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

public class SocialStompErrorHandler extends StompSubProtocolErrorHandler {

    private final ObjectMapper objectMapper;

    public SocialStompErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Message<byte[]> handleClientMessageProcessingError(Message<byte[]> clientMessage, Throwable ex) {
        Throwable cause = unwrap(ex);
        ErrorMeta meta = mapError(cause);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
        accessor.setContentType(MimeTypeUtils.APPLICATION_JSON);
        accessor.setMessage(meta.message());
        accessor.setLeaveMutable(true);

        String receiptId = extractReceiptId(clientMessage);
        if (receiptId != null) {
            accessor.setReceiptId(receiptId);
        }

        byte[] payload = toJsonBytes(Map.of(
            "timestamp", Instant.now().toString(),
            "status", meta.status(),
            "error", meta.error(),
            "message", meta.message(),
            "path", extractDestination(clientMessage)
        ));

        return MessageBuilder.createMessage(payload, accessor.getMessageHeaders());
    }

    private String extractReceiptId(Message<byte[]> clientMessage) {
        if (clientMessage == null) {
            return null;
        }
        StompHeaderAccessor clientAccessor = MessageHeaderAccessor.getAccessor(clientMessage, StompHeaderAccessor.class);
        if (clientAccessor == null) {
            return null;
        }
        return clientAccessor.getReceipt();
    }

    private String extractDestination(Message<byte[]> clientMessage) {
        if (clientMessage == null) {
            return "unknown";
        }
        StompHeaderAccessor clientAccessor = MessageHeaderAccessor.getAccessor(clientMessage, StompHeaderAccessor.class);
        if (clientAccessor == null || clientAccessor.getDestination() == null) {
            return "unknown";
        }
        return clientAccessor.getDestination();
    }

    private Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null
            && (current instanceof MessageDeliveryException || current instanceof MessageHandlingException)) {
            current = current.getCause();
        }
        return current;
    }

    private ErrorMeta mapError(Throwable throwable) {
        if (throwable instanceof MethodArgumentNotValidException
            || throwable instanceof ConstraintViolationException
            || throwable instanceof MessageConversionException
            || throwable instanceof IllegalArgumentException) {
            return new ErrorMeta(400, "validation_error", safeMessage(throwable, "Validation failed"));
        }

        if (throwable instanceof AccessDeniedException || throwable instanceof ForbiddenException) {
            return new ErrorMeta(403, "forbidden", safeMessage(throwable, "Operation is forbidden"));
        }

        if (throwable instanceof UnauthorizedException) {
            return new ErrorMeta(401, "unauthorized", safeMessage(throwable, "Unauthorized"));
        }

        if (throwable instanceof NotFoundException) {
            return new ErrorMeta(404, "not_found", safeMessage(throwable, "Resource not found"));
        }

        if (throwable instanceof ConflictException) {
            return new ErrorMeta(409, "conflict", safeMessage(throwable, "Resource conflict"));
        }

        if (throwable instanceof DomainException) {
            return new ErrorMeta(400, "domain_error", safeMessage(throwable, "Domain error"));
        }

        return new ErrorMeta(500, "internal_error", "Unexpected websocket error");
    }

    private String safeMessage(Throwable throwable, String fallback) {
        if (throwable.getMessage() == null || throwable.getMessage().isBlank()) {
            return fallback;
        }
        return throwable.getMessage();
    }

    private byte[] toJsonBytes(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsBytes(payload);
        } catch (JsonProcessingException ignored) {
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("status", 500);
            fallback.put("error", "serialization_error");
            fallback.put("message", "Failed to render websocket error payload");
            return fallback.toString().getBytes(StandardCharsets.UTF_8);
        }
    }

    private record ErrorMeta(int status, String error, String message) {
    }
}
