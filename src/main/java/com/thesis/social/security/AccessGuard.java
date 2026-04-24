package com.thesis.social.security;

import com.thesis.social.chat.repository.ChatParticipantRepository;
import com.thesis.social.friend.entity.FriendRequestEntity;
import com.thesis.social.friend.repository.FriendRequestRepository;
import java.security.Principal;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("accessGuard")
public class AccessGuard {

    private final ChatParticipantRepository chatParticipantRepository;
    private final FriendRequestRepository friendRequestRepository;

    public AccessGuard(ChatParticipantRepository chatParticipantRepository,
                       FriendRequestRepository friendRequestRepository) {
        this.chatParticipantRepository = chatParticipantRepository;
        this.friendRequestRepository = friendRequestRepository;
    }

    public boolean isSelfOrAdmin(UUID profileId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return false;
        }

        if (authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch("ROLE_ADMIN"::equals)) {
            return true;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedProfile authenticatedProfile) {
            return authenticatedProfile.getProfileId().equals(profileId);
        }

        return false;
    }

    public boolean isNotCurrentProfile(UUID profileId) {
        if (profileId == null) {
            return false;
        }
        UUID currentProfileId = currentAuthenticatedProfileId();
        return currentProfileId != null && !currentProfileId.equals(profileId);
    }

    public boolean isCurrentProfileActiveParticipant(UUID channelId) {
        if (channelId == null) {
            return false;
        }
        UUID profileId = currentAuthenticatedProfileId();
        if (profileId == null) {
            return false;
        }
        return chatParticipantRepository.existsByChannelIdAndProfileId(channelId, profileId);
    }

    public boolean isCurrentProfileFriendRequestParticipant(UUID requestId) {
        if (requestId == null) {
            return false;
        }
        UUID profileId = currentAuthenticatedProfileId();
        if (profileId == null) {
            return false;
        }
        return friendRequestRepository.findById(requestId)
            .map(request -> isParticipant(request, profileId))
            .orElse(false);
    }

    public UUID extractProfileId(Principal principal) {
        if (principal instanceof AuthenticatedProfile authenticatedProfile) {
            return authenticatedProfile.getProfileId();
        }

        throw new IllegalStateException("Invalid authenticated principal");
    }

    private UUID currentAuthenticatedProfileId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedProfile profile)) {
            return null;
        }
        return profile.getProfileId();
    }

    private boolean isParticipant(FriendRequestEntity request, UUID profileId) {
        if (profileId == null) {
            return false;
        }
        return profileId.equals(request.getSenderId()) || profileId.equals(request.getReceiverId());
    }

    public boolean isProfileActiveParticipant(UUID channelId, UUID profileId) {
        if (channelId == null || profileId == null) {
            return false;
        }
        return chatParticipantRepository.existsByChannelIdAndProfileId(channelId, profileId);
    }
}
