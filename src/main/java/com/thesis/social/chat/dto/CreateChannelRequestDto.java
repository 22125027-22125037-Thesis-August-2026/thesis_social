package com.thesis.social.chat.dto;

import com.thesis.social.chat.entity.ChatChannelType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;

public record CreateChannelRequestDto(
    @NotNull ChatChannelType type,
    UUID referenceId,
    @NotEmpty Set<UUID> participantIds
) {
}
