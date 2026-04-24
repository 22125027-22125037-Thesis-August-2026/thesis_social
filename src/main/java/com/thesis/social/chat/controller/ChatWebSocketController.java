package com.thesis.social.chat.controller;

import com.thesis.social.chat.dto.ChatMessageResponseDto;
import com.thesis.social.chat.dto.MarkReadRequestDto;
import com.thesis.social.chat.dto.SendMessageRequestDto;
import com.thesis.social.chat.service.ChatService;
import com.thesis.social.security.PrincipalAccess;
import com.thesis.social.security.AccessGuard; 
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.access.AccessDeniedException; 
import org.springframework.stereotype.Controller;

@Controller
public class ChatWebSocketController {

    private final ChatService chatService;
    private final PrincipalAccess principalAccess;
    private final AccessGuard accessGuard; // 1. Inject AccessGuard

    public ChatWebSocketController(ChatService chatService, PrincipalAccess principalAccess, AccessGuard accessGuard) {
        this.chatService = chatService;
        this.principalAccess = principalAccess;
        this.accessGuard = accessGuard;
    }

    @MessageMapping("/chat.send")
    // 2. @PreAuthorize completely removed
    public ChatMessageResponseDto sendMessage(@Valid @Payload SendMessageRequestDto request, Principal principal) {
        // Extract the ID from the STOMP frame, NOT the SecurityContext
        UUID profileId = principalAccess.profileIdFromPrincipal(principal);
        
        // 3. Manual Security Check
        if (!accessGuard.isProfileActiveParticipant(request.channelId(), profileId)) {
            throw new AccessDeniedException("Not an active participant in this channel");
        }
        
        return chatService.sendMessage(profileId, request);
    }

    @MessageMapping("/chat.read")
    // 2. @PreAuthorize completely removed
    public ChatMessageResponseDto markRead(@Valid @Payload MarkReadRequestDto request, Principal principal) {
        UUID profileId = principalAccess.profileIdFromPrincipal(principal);
        
        // 3. Manual Security Check
        if (!accessGuard.isProfileActiveParticipant(request.channelId(), profileId)) {
            throw new AccessDeniedException("Not an active participant in this channel");
        }
        
        return chatService.markRead(profileId, request.channelId(), request.messageId());
    }
}