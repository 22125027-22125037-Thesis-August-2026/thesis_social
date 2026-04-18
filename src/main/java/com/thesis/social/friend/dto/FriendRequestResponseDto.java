package com.thesis.social.friend.dto;

import com.thesis.social.friend.entity.FriendRequestStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record FriendRequestResponseDto(
    UUID id,
    UUID senderId,
    UUID receiverId,
    FriendRequestStatus status,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
