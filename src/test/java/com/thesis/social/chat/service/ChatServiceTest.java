package com.thesis.social.chat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.thesis.social.chat.dto.ChatMessageResponseDto;
import com.thesis.social.chat.dto.SendMessageRequestDto;
import com.thesis.social.chat.entity.ChatChannelEntity;
import com.thesis.social.chat.entity.ChatChannelType;
import com.thesis.social.chat.entity.ChatParticipantEntity;
import com.thesis.social.chat.entity.MessageEntity;
import com.thesis.social.chat.repository.ChatChannelRepository;
import com.thesis.social.chat.repository.ChatParticipantRepository;
import com.thesis.social.chat.repository.MessageRepository;
import com.thesis.social.common.exception.ForbiddenException;
import com.thesis.social.event.DomainEventPublisher;
import com.thesis.social.event.EventTypes;
import com.thesis.social.friend.repository.FriendshipRepository;
import com.thesis.social.friend.service.FriendService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatChannelRepository chatChannelRepository;
    @Mock
    private ChatParticipantRepository chatParticipantRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private FriendService friendService;
    @Mock
    private FriendshipRepository friendshipRepository;
    @Mock
    private DomainEventPublisher eventPublisher;
    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(
            chatChannelRepository,
            chatParticipantRepository,
            messageRepository,
            friendService,
            friendshipRepository,
            eventPublisher,
            simpMessagingTemplate
        );
    }

    @Test
    void sendMessageShouldPersistNotifyAndPublishEvent() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        SendMessageRequestDto request = new SendMessageRequestDto(channelId, "hello");

        ChatParticipantEntity senderParticipant = new ChatParticipantEntity();
        senderParticipant.setChannelId(channelId);
        senderParticipant.setProfileId(senderId);

        ChatParticipantEntity receiverParticipant = new ChatParticipantEntity();
        receiverParticipant.setChannelId(channelId);
        receiverParticipant.setProfileId(receiverId);

        when(chatParticipantRepository.existsByChannelIdAndProfileId(channelId, senderId)).thenReturn(true);
        when(chatParticipantRepository.findByChannelId(channelId)).thenReturn(List.of(senderParticipant, receiverParticipant));
        when(friendService.isBlockedEitherDirection(senderId, receiverId)).thenReturn(false);
        when(messageRepository.save(any(MessageEntity.class))).thenAnswer(invocation -> {
            MessageEntity saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", messageId);
            ReflectionTestUtils.setField(saved, "createdAt", OffsetDateTime.now());
            ReflectionTestUtils.setField(saved, "updatedAt", OffsetDateTime.now());
            return saved;
        });

        ChatMessageResponseDto response = chatService.sendMessage(senderId, request);

        assertEquals(channelId, response.channelId());
        assertEquals(senderId, response.senderId());
        assertEquals("hello", response.content());
        verify(simpMessagingTemplate).convertAndSendToUser(eq(receiverId.toString()), eq("/queue/messages"), any(ChatMessageResponseDto.class));
        verify(eventPublisher).publish(eq(EventTypes.MESSAGE_SENT), org.mockito.ArgumentMatchers.<Map<String, Object>>any());
    }

    @Test
    void listMessagesShouldRequireChannelMembership() {
        UUID profileId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();

        when(chatParticipantRepository.existsByChannelIdAndProfileId(channelId, profileId)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> chatService.listMessages(profileId, channelId, 0, 20));
    }

    @Test
    void listMessagesShouldReturnMessagePageForParticipant() {
        UUID profileId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        MessageEntity message = new MessageEntity();
        ReflectionTestUtils.setField(message, "id", messageId);
        message.setChannelId(channelId);
        message.setSenderId(profileId);
        message.setContent("hello");
        message.setRead(false);
        ReflectionTestUtils.setField(message, "createdAt", OffsetDateTime.now());
        ReflectionTestUtils.setField(message, "updatedAt", OffsetDateTime.now());

        when(chatParticipantRepository.existsByChannelIdAndProfileId(channelId, profileId)).thenReturn(true);
        when(messageRepository.findByChannelIdOrderByCreatedAtDesc(eq(channelId), eq(PageRequest.of(0, 20))))
            .thenReturn(new PageImpl<>(List.of(message)));

        Page<ChatMessageResponseDto> page = chatService.listMessages(profileId, channelId, 0, 20);

        assertEquals(1, page.getTotalElements());
        assertEquals(messageId, page.getContent().get(0).id());
    }

    @Test
    void markReadShouldUpdateStateAndPublishEvent() {
        UUID profileId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        MessageEntity message = new MessageEntity();
        ReflectionTestUtils.setField(message, "id", messageId);
        message.setChannelId(channelId);
        message.setSenderId(UUID.randomUUID());
        message.setContent("sample");
        message.setRead(false);
        ReflectionTestUtils.setField(message, "createdAt", OffsetDateTime.now());
        ReflectionTestUtils.setField(message, "updatedAt", OffsetDateTime.now());

        when(chatParticipantRepository.existsByChannelIdAndProfileId(channelId, profileId)).thenReturn(true);
        when(messageRepository.findByIdAndChannelId(messageId, channelId)).thenReturn(java.util.Optional.of(message));
        when(messageRepository.save(any(MessageEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatMessageResponseDto response = chatService.markRead(profileId, channelId, messageId);

        assertEquals(true, response.read());
        verify(eventPublisher).publish(eq(EventTypes.MESSAGE_READ), org.mockito.ArgumentMatchers.<Map<String, Object>>any());
    }

    @Test
    void sendMessageShouldFailWhenProfilesAreBlocked() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();

        SendMessageRequestDto request = new SendMessageRequestDto(channelId, "hello");

        ChatParticipantEntity senderParticipant = new ChatParticipantEntity();
        senderParticipant.setChannelId(channelId);
        senderParticipant.setProfileId(senderId);

        ChatParticipantEntity receiverParticipant = new ChatParticipantEntity();
        receiverParticipant.setChannelId(channelId);
        receiverParticipant.setProfileId(receiverId);

        when(chatParticipantRepository.existsByChannelIdAndProfileId(channelId, senderId)).thenReturn(true);
        when(chatParticipantRepository.findByChannelId(channelId)).thenReturn(List.of(senderParticipant, receiverParticipant));
        when(friendService.isBlockedEitherDirection(senderId, receiverId)).thenReturn(true);

        assertThrows(ForbiddenException.class, () -> chatService.sendMessage(senderId, request));
    }

    @Test
    void listChannelsShouldIncludeCounterpartLastMessageAndUnreadCount() {
        UUID profileId = UUID.randomUUID();
        UUID counterpartId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        ChatParticipantEntity selfParticipant = new ChatParticipantEntity();
        selfParticipant.setChannelId(channelId);
        selfParticipant.setProfileId(profileId);

        ChatParticipantEntity counterpartParticipant = new ChatParticipantEntity();
        counterpartParticipant.setChannelId(channelId);
        counterpartParticipant.setProfileId(counterpartId);

        ChatChannelEntity channel = new ChatChannelEntity();
        ReflectionTestUtils.setField(channel, "id", channelId);
        channel.setType(ChatChannelType.DIRECT_FRIEND);

        MessageEntity lastMessage = new MessageEntity();
        ReflectionTestUtils.setField(lastMessage, "id", messageId);
        lastMessage.setChannelId(channelId);
        lastMessage.setSenderId(counterpartId);
        lastMessage.setContent("latest");
        ReflectionTestUtils.setField(lastMessage, "createdAt", now);
        ReflectionTestUtils.setField(lastMessage, "updatedAt", now);

        when(chatParticipantRepository.findByProfileId(profileId)).thenReturn(List.of(selfParticipant));
        when(chatChannelRepository.findAllById(List.of(channelId))).thenReturn(List.of(channel));
        when(chatParticipantRepository.findByChannelId(channelId)).thenReturn(List.of(selfParticipant, counterpartParticipant));
        when(messageRepository.findFirstByChannelIdOrderByCreatedAtDesc(channelId)).thenReturn(java.util.Optional.of(lastMessage));
        when(messageRepository.countByChannelIdAndIsReadFalseAndSenderIdNot(channelId, profileId)).thenReturn(3L);

        var response = chatService.listChannels(profileId);

        assertEquals(1, response.size());
        assertEquals(channelId, response.get(0).channelId());
        assertEquals(counterpartId, response.get(0).counterpartProfileId());
        assertEquals("latest", response.get(0).lastMessagePreview());
        assertEquals(now, response.get(0).lastMessageAt());
        assertEquals(3L, response.get(0).unreadCount());
    }
}
