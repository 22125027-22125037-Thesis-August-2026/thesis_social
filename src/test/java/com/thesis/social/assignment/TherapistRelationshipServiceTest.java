package com.thesis.social.assignment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.thesis.social.chat.service.DirectChannelService;
import com.thesis.social.common.util.UuidOrdering;
import com.thesis.social.friend.entity.FriendshipEntity;
import com.thesis.social.friend.repository.FriendshipRepository;
import com.thesis.social.friend.service.FriendService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TherapistRelationshipServiceTest {

    @Mock
    private FriendshipRepository friendshipRepository;
    @Mock
    private DirectChannelService directChannelService;
    @Mock
    private FriendService friendService;

    private TherapistRelationshipService service;

    private final UUID therapistId = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new TherapistRelationshipService(friendshipRepository, directChannelService, friendService);
    }

    private UUID sortedFirst() {
        return UuidOrdering.UNSIGNED.compare(therapistId, patientId) < 0 ? therapistId : patientId;
    }

    private UUID sortedSecond() {
        return UuidOrdering.UNSIGNED.compare(therapistId, patientId) < 0 ? patientId : therapistId;
    }

    @Test
    void linkShouldCreateFriendshipAndDirectChannel() {
        when(friendService.isBlockedEitherDirection(therapistId, patientId)).thenReturn(false);
        when(friendshipRepository.existsByProfileId1AndProfileId2(sortedFirst(), sortedSecond())).thenReturn(false);

        service.linkTherapistAndPatient(therapistId, patientId);

        ArgumentCaptor<FriendshipEntity> captor = ArgumentCaptor.forClass(FriendshipEntity.class);
        verify(friendshipRepository).save(captor.capture());
        verify(directChannelService).ensureDirectFriendChannel(therapistId, patientId);

        FriendshipEntity saved = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(sortedFirst(), saved.getProfileId1());
        org.junit.jupiter.api.Assertions.assertEquals(sortedSecond(), saved.getProfileId2());
    }

    @Test
    void linkShouldNotDuplicateExistingFriendship() {
        when(friendService.isBlockedEitherDirection(therapistId, patientId)).thenReturn(false);
        when(friendshipRepository.existsByProfileId1AndProfileId2(sortedFirst(), sortedSecond())).thenReturn(true);

        service.linkTherapistAndPatient(therapistId, patientId);

        verify(friendshipRepository, never()).save(any());
        verify(directChannelService).ensureDirectFriendChannel(therapistId, patientId);
    }

    @Test
    void linkShouldSkipWhenBlocked() {
        when(friendService.isBlockedEitherDirection(therapistId, patientId)).thenReturn(true);

        service.linkTherapistAndPatient(therapistId, patientId);

        verify(friendshipRepository, never()).save(any());
        verify(directChannelService, never()).ensureDirectFriendChannel(any(), any());
    }

    @Test
    void linkShouldIgnoreSelfAssignment() {
        service.linkTherapistAndPatient(therapistId, therapistId);

        verify(friendshipRepository, never()).save(any());
        verify(directChannelService, never()).ensureDirectFriendChannel(any(), any());
    }
}
