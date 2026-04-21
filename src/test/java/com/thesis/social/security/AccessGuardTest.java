package com.thesis.social.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.thesis.social.chat.repository.ChatParticipantRepository;
import com.thesis.social.friend.entity.FriendRequestEntity;
import com.thesis.social.friend.repository.FriendRequestRepository;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AccessGuardTest {

    @Mock
    private ChatParticipantRepository chatParticipantRepository;

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @InjectMocks
    private AccessGuard accessGuard;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void isCurrentProfileActiveParticipantShouldReturnTrueForParticipant() {
        UUID profileId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();
        setAuthentication(profileId);
        when(chatParticipantRepository.existsByChannelIdAndProfileId(channelId, profileId)).thenReturn(true);

        assertTrue(accessGuard.isCurrentProfileActiveParticipant(channelId));
    }

    @Test
    void isCurrentProfileActiveParticipantShouldReturnFalseWhenNotAuthenticated() {
        assertFalse(accessGuard.isCurrentProfileActiveParticipant(UUID.randomUUID()));
    }

    @Test
    void isCurrentProfileFriendRequestParticipantShouldReturnTrueWhenSenderMatches() {
        UUID profileId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        setAuthentication(profileId);

        FriendRequestEntity request = new FriendRequestEntity();
        request.setSenderId(profileId);
        request.setReceiverId(UUID.randomUUID());
        when(friendRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

        assertTrue(accessGuard.isCurrentProfileFriendRequestParticipant(requestId));
    }

    @Test
    void isNotCurrentProfileShouldRejectSelfProfile() {
        UUID profileId = UUID.randomUUID();
        setAuthentication(profileId);

        assertFalse(accessGuard.isNotCurrentProfile(profileId));
        assertTrue(accessGuard.isNotCurrentProfile(UUID.randomUUID()));
    }

    private void setAuthentication(UUID profileId) {
        AuthenticatedProfile principal = new AuthenticatedProfile(
            profileId,
            Set.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
