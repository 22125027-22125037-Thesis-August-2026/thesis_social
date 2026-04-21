package com.thesis.social.chat.controller;

import com.thesis.social.chat.dto.ChatChannelResponseDto;
import com.thesis.social.chat.dto.ChatMessageResponseDto;
import com.thesis.social.chat.dto.CreateChannelRequestDto;
import com.thesis.social.chat.service.ChatService;
import com.thesis.social.security.AuthenticatedProfile;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/chats")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/channels")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ChatChannelResponseDto createChannel(
        @AuthenticationPrincipal AuthenticatedProfile profile,
        @Valid @RequestBody CreateChannelRequestDto request
    ) {
        return chatService.createChannel(profile.getProfileId(), request);
    }

    @GetMapping("/channels")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public List<ChatChannelResponseDto> listChannels(@AuthenticationPrincipal AuthenticatedProfile profile) {
        return chatService.listChannels(profile.getProfileId());
    }

    @GetMapping("/channels/{channelId}/messages")
    @PreAuthorize("hasAnyRole('USER','ADMIN') and @accessGuard.isCurrentProfileActiveParticipant(#channelId)")
    public Page<ChatMessageResponseDto> listMessages(
        @AuthenticationPrincipal AuthenticatedProfile profile,
        @PathVariable UUID channelId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return chatService.listMessages(profile.getProfileId(), channelId, page, size);
    }

    @PatchMapping("/channels/{channelId}/messages/{messageId}/read")
    @PreAuthorize("hasAnyRole('USER','ADMIN') and @accessGuard.isCurrentProfileActiveParticipant(#channelId)")
    public ChatMessageResponseDto markRead(
        @AuthenticationPrincipal AuthenticatedProfile profile,
        @PathVariable UUID channelId,
        @PathVariable UUID messageId
    ) {
        return chatService.markRead(profile.getProfileId(), channelId, messageId);
    }
}
