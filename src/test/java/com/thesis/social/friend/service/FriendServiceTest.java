package com.thesis.social.friend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.thesis.social.common.exception.ForbiddenException;
import com.thesis.social.event.DomainEventPublisher;
import com.thesis.social.event.EventTypes;
import com.thesis.social.friend.dto.FriendRequestResponseDto;
import com.thesis.social.friend.entity.FriendRequestEntity;
import com.thesis.social.friend.entity.FriendRequestStatus;
import com.thesis.social.friend.repository.FriendRequestRepository;
import com.thesis.social.friend.repository.FriendshipRepository;
import com.thesis.social.friend.repository.ProfileBlockRepository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FriendServiceTest {

    @Mock
    private FriendRequestRepository friendRequestRepository;
    @Mock
    private FriendshipRepository friendshipRepository;
    @Mock
    private ProfileBlockRepository profileBlockRepository;
    @Mock
    private DomainEventPublisher eventPublisher;

    private FriendService friendService;

    @BeforeEach
    void setUp() {
        friendService = new FriendService(friendRequestRepository, friendshipRepository, profileBlockRepository, eventPublisher);
    }

    @Test
    void sendRequestShouldFailWhenBlocked() {
        UUID sender = UUID.randomUUID();
        UUID receiver = UUID.randomUUID();

        when(profileBlockRepository.existsByBlockerIdAndBlockedId(sender, receiver)).thenReturn(true);

        assertThrows(ForbiddenException.class, () -> friendService.sendRequest(sender, receiver));
    }

    @Test
    void acceptRequestShouldCreateFriendshipAndPublishEvent() {
        UUID sender = UUID.randomUUID();
        UUID receiver = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        FriendRequestEntity request = new FriendRequestEntity();
        request.setSenderId(sender);
        request.setReceiverId(receiver);
        request.setStatus(FriendRequestStatus.PENDING);

        when(friendRequestRepository.findByIdAndStatus(requestId, FriendRequestStatus.PENDING)).thenReturn(Optional.of(request));
        when(friendRequestRepository.save(any(FriendRequestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(profileBlockRepository.existsByBlockerIdAndBlockedId(any(UUID.class), any(UUID.class))).thenReturn(false);
        when(friendshipRepository.existsByProfileId1AndProfileId2(any(UUID.class), any(UUID.class))).thenReturn(false);

        FriendRequestResponseDto response = friendService.acceptRequest(receiver, requestId);

        assertEquals(FriendRequestStatus.ACCEPTED, response.status());
        verify(friendshipRepository).save(any());
        verify(eventPublisher).publish(eq(EventTypes.FRIEND_REQUEST_ACCEPTED), org.mockito.ArgumentMatchers.<Map<String, Object>>any());
    }
}
