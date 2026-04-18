package com.thesis.social.chat.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record MarkReadRequestDto(
	@NotNull UUID channelId,
	@NotNull UUID messageId
) {
}
