package com.thesis.social.chat.dto;

import com.thesis.social.chat.entity.ChatChannelType;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

public record ChatChannelResponseDto(
    UUID id,
    ChatChannelType type,
    UUID referenceId,
    Set<UUID> participantIds,
    OffsetDateTime createdAt
) {
}
