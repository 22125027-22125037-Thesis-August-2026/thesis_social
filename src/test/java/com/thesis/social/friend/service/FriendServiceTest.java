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
import com.thesis.social.friend.dto.FriendRequestDirection;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

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
        ReflectionTestUtils.setField(request, "id", requestId);
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

    @Test
    void sendRequestShouldCreatePendingAndPublishEvent() {
        UUID sender = UUID.randomUUID();
        UUID receiver = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        when(profileBlockRepository.existsByBlockerIdAndBlockedId(any(UUID.class), any(UUID.class))).thenReturn(false);
        when(friendshipRepository.existsByProfileId1AndProfileId2(any(UUID.class), any(UUID.class))).thenReturn(false);
        when(friendRequestRepository.existsBySenderIdAndReceiverIdAndStatus(any(UUID.class), any(UUID.class), eq(FriendRequestStatus.PENDING)))
            .thenReturn(false);

        when(friendRequestRepository.save(any(FriendRequestEntity.class))).thenAnswer(invocation -> {
            FriendRequestEntity saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", requestId);
            return saved;
        });

        FriendRequestResponseDto response = friendService.sendRequest(sender, receiver);

        assertEquals(FriendRequestStatus.PENDING, response.status());
        verify(eventPublisher).publish(eq(EventTypes.FRIEND_REQUEST_CREATED), org.mockito.ArgumentMatchers.<Map<String, Object>>any());
    }

    @Test
    void rejectRequestShouldUpdateStatus() {
        UUID sender = UUID.randomUUID();
        UUID receiver = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        FriendRequestEntity request = new FriendRequestEntity();
        ReflectionTestUtils.setField(request, "id", requestId);
        request.setSenderId(sender);
        request.setReceiverId(receiver);
        request.setStatus(FriendRequestStatus.PENDING);

        when(friendRequestRepository.findByIdAndStatus(requestId, FriendRequestStatus.PENDING)).thenReturn(Optional.of(request));
        when(friendRequestRepository.save(any(FriendRequestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FriendRequestResponseDto response = friendService.rejectRequest(receiver, requestId);
        assertEquals(FriendRequestStatus.REJECTED, response.status());
    }

    @Test
    void cancelRequestShouldUpdateStatus() {
        UUID sender = UUID.randomUUID();
        UUID receiver = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        FriendRequestEntity request = new FriendRequestEntity();
        ReflectionTestUtils.setField(request, "id", requestId);
        request.setSenderId(sender);
        request.setReceiverId(receiver);
        request.setStatus(FriendRequestStatus.PENDING);

        when(friendRequestRepository.findByIdAndStatus(requestId, FriendRequestStatus.PENDING)).thenReturn(Optional.of(request));
        when(friendRequestRepository.save(any(FriendRequestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FriendRequestResponseDto response = friendService.cancelRequest(sender, requestId);
        assertEquals(FriendRequestStatus.CANCELED, response.status());
    }

    @Test
    void listIncomingRequestsShouldReturnOnlyPendingIncomingPage() {
        UUID receiver = UUID.randomUUID();
        UUID sender = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        FriendRequestEntity request = new FriendRequestEntity();
        ReflectionTestUtils.setField(request, "id", requestId);
        request.setSenderId(sender);
        request.setReceiverId(receiver);
        request.setStatus(FriendRequestStatus.PENDING);

        when(friendRequestRepository.findByReceiverIdAndStatusOrderByCreatedAtDesc(
            receiver,
            FriendRequestStatus.PENDING,
            PageRequest.of(0, 20)
        )).thenReturn(new PageImpl<>(java.util.List.of(request)));

        var page = friendService.listIncomingRequests(receiver, 0, 20);

        assertEquals(1, page.getTotalElements());
        assertEquals(requestId, page.getContent().get(0).id());
    }

    @Test
    void listRequestsShouldDelegateByDirection() {
        UUID profileId = UUID.randomUUID();

        when(friendRequestRepository.findBySenderIdAndStatusOrderByCreatedAtDesc(
            profileId,
            FriendRequestStatus.PENDING,
            PageRequest.of(0, 20)
        )).thenReturn(new PageImpl<>(java.util.List.of()));

        var page = friendService.listRequests(profileId, FriendRequestDirection.OUTGOING, 0, 20);

        assertEquals(0, page.getTotalElements());
        verify(friendRequestRepository).findBySenderIdAndStatusOrderByCreatedAtDesc(
            profileId,
            FriendRequestStatus.PENDING,
            PageRequest.of(0, 20)
        );
    }
}
