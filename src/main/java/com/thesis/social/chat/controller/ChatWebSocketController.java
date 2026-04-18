package com.thesis.social.chat.controller;

import com.thesis.social.chat.dto.ChatMessageResponseDto;
import com.thesis.social.chat.dto.MarkReadRequestDto;
import com.thesis.social.chat.dto.SendMessageRequestDto;
import com.thesis.social.chat.service.ChatService;
import com.thesis.social.security.PrincipalAccess;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
public class ChatWebSocketController {

    private final ChatService chatService;
    private final PrincipalAccess principalAccess;

    public ChatWebSocketController(ChatService chatService, PrincipalAccess principalAccess) {
        this.chatService = chatService;
        this.principalAccess = principalAccess;
    }

    @MessageMapping("/chat.send")
    public ChatMessageResponseDto sendMessage(@Valid @Payload SendMessageRequestDto request, Principal principal) {
        UUID profileId = principalAccess.profileIdFromPrincipal(principal);
        return chatService.sendMessage(profileId, request);
    }

    @MessageMapping("/chat.read")
    public ChatMessageResponseDto markRead(@Valid @Payload MarkReadRequestDto request, Principal principal) {
        UUID profileId = principalAccess.profileIdFromPrincipal(principal);
        return chatService.markRead(profileId, request.channelId(), request.messageId());
    }
}
