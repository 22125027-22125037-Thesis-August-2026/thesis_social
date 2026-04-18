package com.thesis.social.friend.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SendFriendRequestDto(
    @NotNull UUID receiverId
) {
}
