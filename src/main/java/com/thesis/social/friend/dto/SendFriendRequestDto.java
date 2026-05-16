package com.thesis.social.friend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendFriendRequestDto(
    @NotBlank @Email @Size(max = 255) String receiverEmail
) {
}
