package com.thesis.social.assignment;

import com.thesis.social.chat.service.DirectChannelService;
import com.thesis.social.common.util.UuidOrdering;
import com.thesis.social.friend.entity.FriendshipEntity;
import com.thesis.social.friend.repository.FriendshipRepository;
import com.thesis.social.friend.service.FriendService;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the social-side relationship for a therapist&lt;-&gt;patient match: a friendship row
 * plus their direct chat channel. Idempotent — replayed events find the existing friendship
 * and channel and change nothing.
 *
 * <p>Deactivations are intentionally not handled here: when a patient is re-matched, the chat
 * and relationship with the previous therapist are kept.
 */
@Service
public class TherapistRelationshipService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TherapistRelationshipService.class);

    private final FriendshipRepository friendshipRepository;
    private final DirectChannelService directChannelService;
    private final FriendService friendService;

    public TherapistRelationshipService(FriendshipRepository friendshipRepository,
                                        DirectChannelService directChannelService,
                                        FriendService friendService) {
        this.friendshipRepository = friendshipRepository;
        this.directChannelService = directChannelService;
        this.friendService = friendService;
    }

    @Transactional
    public void linkTherapistAndPatient(UUID therapistProfileId, UUID patientProfileId) {
        if (therapistProfileId.equals(patientProfileId)) {
            LOGGER.warn("Ignoring self-assignment for profile {}", therapistProfileId);
            return;
        }

        if (friendService.isBlockedEitherDirection(therapistProfileId, patientProfileId)) {
            LOGGER.warn("Skipping therapist link for therapist={} patient={}: profiles are blocked",
                    therapistProfileId, patientProfileId);
            return;
        }

        boolean therapistFirst = UuidOrdering.UNSIGNED.compare(therapistProfileId, patientProfileId) < 0;
        UUID first = therapistFirst ? therapistProfileId : patientProfileId;
        UUID second = therapistFirst ? patientProfileId : therapistProfileId;

        if (!friendshipRepository.existsByProfileId1AndProfileId2(first, second)) {
            FriendshipEntity friendship = new FriendshipEntity();
            friendship.setProfileId1(first);
            friendship.setProfileId2(second);
            friendshipRepository.save(friendship);
        }

        directChannelService.ensureDirectFriendChannel(therapistProfileId, patientProfileId);

        LOGGER.info("Linked therapist {} and patient {} (friendship + direct channel)",
                therapistProfileId, patientProfileId);
    }
}
