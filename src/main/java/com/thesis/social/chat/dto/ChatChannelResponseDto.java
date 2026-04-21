package com.thesis.social.chat.dto;

import com.thesis.social.chat.entity.ChatChannelType;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

public record ChatChannelResponseDto(
    UUID channelId,
    UUID id,
    ChatChannelType type,
    UUID referenceId,
    Set<UUID> participantIds,
    OffsetDateTime createdAt,
    UUID counterpartProfileId,
    String counterpartDisplayName,
    String counterpartAvatarUrl,
    String lastMessagePreview,
    OffsetDateTime lastMessageAt,
    long unreadCount,
    String moodAlert,
    String checkInPrompt
) {
}
