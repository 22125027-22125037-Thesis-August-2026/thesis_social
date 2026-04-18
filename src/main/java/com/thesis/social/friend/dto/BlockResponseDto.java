package com.thesis.social.friend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BlockResponseDto(
    UUID blockerId,
    UUID blockedId,
    OffsetDateTime createdAt
) {
}
