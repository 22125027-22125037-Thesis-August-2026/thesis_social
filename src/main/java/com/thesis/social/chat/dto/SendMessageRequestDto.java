package com.thesis.social.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SendMessageRequestDto(
    @NotNull UUID channelId,
    @NotBlank @Size(max = 4000) String content
) {
}
