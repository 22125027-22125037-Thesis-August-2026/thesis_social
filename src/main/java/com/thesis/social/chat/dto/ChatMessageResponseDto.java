package com.thesis.social.chat.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ChatMessageResponseDto(
    UUID id,
    UUID channelId,
    UUID senderId,
    String senderUsername,
    String content,
    boolean read,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
