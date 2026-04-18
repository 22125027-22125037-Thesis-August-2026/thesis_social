package com.thesis.social.chat.service;

import com.thesis.social.chat.dto.ChatChannelResponseDto;
import com.thesis.social.chat.dto.ChatMessageResponseDto;
import com.thesis.social.chat.dto.CreateChannelRequestDto;
import com.thesis.social.chat.dto.SendMessageRequestDto;
import com.thesis.social.chat.entity.ChatChannelEntity;
import com.thesis.social.chat.entity.ChatChannelType;
import com.thesis.social.chat.entity.ChatParticipantEntity;
import com.thesis.social.chat.entity.MessageEntity;
import com.thesis.social.chat.repository.ChatChannelRepository;
import com.thesis.social.chat.repository.ChatParticipantRepository;
import com.thesis.social.chat.repository.MessageRepository;
import com.thesis.social.common.exception.ConflictException;
import com.thesis.social.common.exception.ForbiddenException;
import com.thesis.social.common.exception.NotFoundException;
import com.thesis.social.event.DomainEventPublisher;
import com.thesis.social.event.EventTypes;
import com.thesis.social.friend.repository.FriendshipRepository;
import com.thesis.social.friend.service.FriendService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {

    private static final String USER_QUEUE_MESSAGES = "/queue/messages";

    private final ChatChannelRepository chatChannelRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final MessageRepository messageRepository;
    private final FriendService friendService;
    private final FriendshipRepository friendshipRepository;
    private final DomainEventPublisher eventPublisher;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public ChatService(ChatChannelRepository chatChannelRepository,
                       ChatParticipantRepository chatParticipantRepository,
                       MessageRepository messageRepository,
                       FriendService friendService,
                       FriendshipRepository friendshipRepository,
                       DomainEventPublisher eventPublisher,
                       SimpMessagingTemplate simpMessagingTemplate) {
        this.chatChannelRepository = chatChannelRepository;
        this.chatParticipantRepository = chatParticipantRepository;
        this.messageRepository = messageRepository;
        this.friendService = friendService;
        this.friendshipRepository = friendshipRepository;
        this.eventPublisher = eventPublisher;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    @Transactional
    public ChatChannelResponseDto createChannel(UUID requesterId, CreateChannelRequestDto request) {
        Set<UUID> participants = request.participantIds().stream().collect(Collectors.toSet());
        participants.add(requesterId);

        if (request.type() == ChatChannelType.DIRECT_FRIEND) {
            validateDirectFriendChannel(participants);
        }

        ChatChannelEntity channel = new ChatChannelEntity();
        channel.setType(request.type());
        channel.setReferenceId(request.referenceId());
        ChatChannelEntity savedChannel = chatChannelRepository.save(channel);

        for (UUID participantId : participants) {
            ChatParticipantEntity participant = new ChatParticipantEntity();
            participant.setChannelId(savedChannel.getId());
            participant.setProfileId(participantId);
            chatParticipantRepository.save(participant);
        }

        return toChannelDto(savedChannel, participants);
    }

    @Transactional(readOnly = true)
    public List<ChatChannelResponseDto> listChannels(UUID profileId) {
        List<ChatParticipantEntity> participants = chatParticipantRepository.findByProfileId(profileId);
        List<UUID> channelIds = participants.stream().map(ChatParticipantEntity::getChannelId).distinct().toList();

        return chatChannelRepository.findAllById(channelIds).stream()
            .map(channel -> {
                Set<UUID> participantIds = chatParticipantRepository.findByChannelId(channel.getId())
                    .stream()
                    .map(ChatParticipantEntity::getProfileId)
                    .collect(Collectors.toSet());
                return toChannelDto(channel, participantIds);
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public Page<ChatMessageResponseDto> listMessages(UUID profileId, UUID channelId, int page, int size) {
        ensureMembership(profileId, channelId);
        Pageable pageable = PageRequest.of(page, size);
        return messageRepository.findByChannelIdOrderByCreatedAtDesc(channelId, pageable)
            .map(this::toMessageDto);
    }

    @Transactional
    public ChatMessageResponseDto sendMessage(UUID senderId, SendMessageRequestDto request) {
        ensureMembership(senderId, request.channelId());

        List<UUID> otherParticipants = chatParticipantRepository.findByChannelId(request.channelId()).stream()
            .map(ChatParticipantEntity::getProfileId)
            .filter(participant -> !participant.equals(senderId))
            .toList();

        for (UUID participantId : otherParticipants) {
            if (friendService.isBlockedEitherDirection(senderId, participantId)) {
                throw new ForbiddenException("Cannot send message due to profile block");
            }
        }

        MessageEntity entity = new MessageEntity();
        entity.setChannelId(request.channelId());
        entity.setSenderId(senderId);
        entity.setContent(request.content().trim());
        entity.setRead(false);
        MessageEntity saved = messageRepository.save(entity);

        ChatMessageResponseDto dto = toMessageDto(saved);
        for (UUID participantId : otherParticipants) {
            simpMessagingTemplate.convertAndSendToUser(participantId.toString(), USER_QUEUE_MESSAGES, dto);
        }

        publishEvent(EventTypes.MESSAGE_SENT, Map.of(
            "messageId", saved.getId(),
            "channelId", saved.getChannelId(),
            "senderId", saved.getSenderId()
        ));

        return dto;
    }

    @Transactional
    public ChatMessageResponseDto markRead(UUID profileId, UUID channelId, UUID messageId) {
        ensureMembership(profileId, channelId);

        MessageEntity message = messageRepository.findByIdAndChannelId(messageId, channelId)
            .orElseThrow(() -> new NotFoundException("Message not found in channel"));

        if (!message.isRead()) {
            message.setRead(true);
            messageRepository.save(message);
            publishEvent(EventTypes.MESSAGE_READ, Map.of(
                "messageId", message.getId(),
                "channelId", message.getChannelId(),
                "readerId", profileId
            ));
        }

        return toMessageDto(message);
    }

    private void ensureMembership(UUID profileId, UUID channelId) {
        if (!chatParticipantRepository.existsByChannelIdAndProfileId(channelId, profileId)) {
            throw new ForbiddenException("Profile is not a participant of this channel");
        }
    }

    private void validateDirectFriendChannel(Set<UUID> participants) {
        if (participants.size() != 2) {
            throw new ConflictException("DIRECT_FRIEND channel requires exactly two participants");
        }
        UUID first = participants.stream().findFirst().orElseThrow();
        UUID second = participants.stream().skip(1).findFirst().orElseThrow();

        if (friendService.isBlockedEitherDirection(first, second)) {
            throw new ForbiddenException("Cannot create direct channel between blocked profiles");
        }

        Pair pair = sortedPair(first, second);
        boolean areFriends = friendshipRepository.existsByProfileId1AndProfileId2(pair.first(), pair.second());
        if (!areFriends) {
            throw new ConflictException("DIRECT_FRIEND channel requires friendship");
        }
    }

    private ChatChannelResponseDto toChannelDto(ChatChannelEntity channel, Set<UUID> participantIds) {
        return new ChatChannelResponseDto(
            channel.getId(),
            channel.getType(),
            channel.getReferenceId(),
            participantIds,
            channel.getCreatedAt()
        );
    }

    private ChatMessageResponseDto toMessageDto(MessageEntity entity) {
        return new ChatMessageResponseDto(
            entity.getId(),
            entity.getChannelId(),
            entity.getSenderId(),
            entity.getContent(),
            entity.isRead(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    private Pair sortedPair(UUID first, UUID second) {
        if (first.compareTo(second) < 0) {
            return new Pair(first, second);
        }
        return new Pair(second, first);
    }

    private void publishEvent(String type, Map<String, Object> payload) {
        Map<String, Object> eventPayload = new HashMap<>(payload);
        eventPayload.put("producer", "social-features-service");
        eventPublisher.publish(type, eventPayload);
    }

    private record Pair(UUID first, UUID second) {
    }
}
