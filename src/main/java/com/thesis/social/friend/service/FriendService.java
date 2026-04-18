package com.thesis.social.friend.service;

import com.thesis.social.common.exception.ConflictException;
import com.thesis.social.common.exception.ForbiddenException;
import com.thesis.social.common.exception.NotFoundException;
import com.thesis.social.event.DomainEventPublisher;
import com.thesis.social.event.EventTypes;
import com.thesis.social.friend.dto.BlockResponseDto;
import com.thesis.social.friend.dto.FriendDto;
import com.thesis.social.friend.dto.FriendRequestResponseDto;
import com.thesis.social.friend.entity.FriendRequestEntity;
import com.thesis.social.friend.entity.FriendRequestStatus;
import com.thesis.social.friend.entity.FriendshipEntity;
import com.thesis.social.friend.entity.ProfileBlockEntity;
import com.thesis.social.friend.repository.FriendRequestRepository;
import com.thesis.social.friend.repository.FriendshipRepository;
import com.thesis.social.friend.repository.ProfileBlockRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FriendService {

    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final ProfileBlockRepository profileBlockRepository;
    private final DomainEventPublisher domainEventPublisher;

    public FriendService(FriendRequestRepository friendRequestRepository,
                         FriendshipRepository friendshipRepository,
                         ProfileBlockRepository profileBlockRepository,
                         DomainEventPublisher domainEventPublisher) {
        this.friendRequestRepository = friendRequestRepository;
        this.friendshipRepository = friendshipRepository;
        this.profileBlockRepository = profileBlockRepository;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Transactional
    public FriendRequestResponseDto sendRequest(UUID senderId, UUID receiverId) {
        validateDistinctProfiles(senderId, receiverId);
        ensureNotBlockedEitherDirection(senderId, receiverId);

        Pair pair = sortedPair(senderId, receiverId);
        if (friendshipRepository.existsByProfileId1AndProfileId2(pair.first(), pair.second())) {
            throw new ConflictException("Profiles are already friends");
        }

        if (friendRequestRepository.existsBySenderIdAndReceiverIdAndStatus(senderId, receiverId, FriendRequestStatus.PENDING)
            || friendRequestRepository.existsBySenderIdAndReceiverIdAndStatus(receiverId, senderId, FriendRequestStatus.PENDING)) {
            throw new ConflictException("Pending request already exists");
        }

        FriendRequestEntity request = new FriendRequestEntity();
        request.setSenderId(senderId);
        request.setReceiverId(receiverId);
        request.setStatus(FriendRequestStatus.PENDING);

        FriendRequestEntity saved = friendRequestRepository.save(request);
        publishEvent(EventTypes.FRIEND_REQUEST_CREATED, Map.of(
            "friendRequestId", saved.getId(),
            "senderId", senderId,
            "receiverId", receiverId
        ));

        return toDto(saved);
    }

    @Transactional
    public FriendRequestResponseDto cancelRequest(UUID senderId, UUID requestId) {
        FriendRequestEntity request = friendRequestRepository.findByIdAndStatus(requestId, FriendRequestStatus.PENDING)
            .orElseThrow(() -> new NotFoundException("Pending friend request not found"));

        if (!request.getSenderId().equals(senderId)) {
            throw new ForbiddenException("Only the sender can cancel this friend request");
        }

        request.setStatus(FriendRequestStatus.CANCELED);
        return toDto(friendRequestRepository.save(request));
    }

    @Transactional
    public FriendRequestResponseDto acceptRequest(UUID receiverId, UUID requestId) {
        FriendRequestEntity request = friendRequestRepository.findByIdAndStatus(requestId, FriendRequestStatus.PENDING)
            .orElseThrow(() -> new NotFoundException("Pending friend request not found"));

        if (!request.getReceiverId().equals(receiverId)) {
            throw new ForbiddenException("Only the receiver can accept this friend request");
        }

        ensureNotBlockedEitherDirection(request.getSenderId(), request.getReceiverId());

        request.setStatus(FriendRequestStatus.ACCEPTED);
        FriendRequestEntity savedRequest = friendRequestRepository.save(request);

        Pair pair = sortedPair(request.getSenderId(), request.getReceiverId());
        if (!friendshipRepository.existsByProfileId1AndProfileId2(pair.first(), pair.second())) {
            FriendshipEntity friendshipEntity = new FriendshipEntity();
            friendshipEntity.setProfileId1(pair.first());
            friendshipEntity.setProfileId2(pair.second());
            friendshipRepository.save(friendshipEntity);
        }

        publishEvent(EventTypes.FRIEND_REQUEST_ACCEPTED, Map.of(
            "friendRequestId", savedRequest.getId(),
            "senderId", savedRequest.getSenderId(),
            "receiverId", savedRequest.getReceiverId()
        ));

        return toDto(savedRequest);
    }

    @Transactional
    public FriendRequestResponseDto rejectRequest(UUID receiverId, UUID requestId) {
        FriendRequestEntity request = friendRequestRepository.findByIdAndStatus(requestId, FriendRequestStatus.PENDING)
            .orElseThrow(() -> new NotFoundException("Pending friend request not found"));

        if (!request.getReceiverId().equals(receiverId)) {
            throw new ForbiddenException("Only the receiver can reject this friend request");
        }

        request.setStatus(FriendRequestStatus.REJECTED);
        return toDto(friendRequestRepository.save(request));
    }

    @Transactional
    public void unfriend(UUID profileId, UUID otherProfileId) {
        validateDistinctProfiles(profileId, otherProfileId);
        Pair pair = sortedPair(profileId, otherProfileId);
        FriendshipEntity friendship = friendshipRepository.findByProfileId1AndProfileId2(pair.first(), pair.second())
            .orElseThrow(() -> new NotFoundException("Friendship not found"));
        friendshipRepository.delete(friendship);
    }

    @Transactional(readOnly = true)
    public List<FriendDto> listFriends(UUID profileId) {
        return friendshipRepository.findByProfileId1OrProfileId2(profileId, profileId)
            .stream()
            .map(friendship -> {
                UUID friendProfileId = friendship.getProfileId1().equals(profileId)
                    ? friendship.getProfileId2() : friendship.getProfileId1();
                return new FriendDto(friendProfileId);
            })
            .toList();
    }

    @Transactional
    public BlockResponseDto block(UUID blockerId, UUID blockedId) {
        validateDistinctProfiles(blockerId, blockedId);

        if (profileBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            throw new ConflictException("Profile already blocked");
        }

        ProfileBlockEntity block = new ProfileBlockEntity();
        block.setBlockerId(blockerId);
        block.setBlockedId(blockedId);
        ProfileBlockEntity saved = profileBlockRepository.save(block);

        Pair pair = sortedPair(blockerId, blockedId);
        friendshipRepository.findByProfileId1AndProfileId2(pair.first(), pair.second())
            .ifPresent(friendshipRepository::delete);

        return new BlockResponseDto(saved.getBlockerId(), saved.getBlockedId(), saved.getCreatedAt());
    }

    @Transactional
    public void unblock(UUID blockerId, UUID blockedId) {
        ProfileBlockEntity block = profileBlockRepository.findByBlockerIdAndBlockedId(blockerId, blockedId)
            .orElseThrow(() -> new NotFoundException("Block relationship not found"));
        profileBlockRepository.delete(block);
    }

    public boolean isBlockedEitherDirection(UUID firstProfileId, UUID secondProfileId) {
        return profileBlockRepository.existsByBlockerIdAndBlockedId(firstProfileId, secondProfileId)
            || profileBlockRepository.existsByBlockerIdAndBlockedId(secondProfileId, firstProfileId);
    }

    private void ensureNotBlockedEitherDirection(UUID senderId, UUID receiverId) {
        if (isBlockedEitherDirection(senderId, receiverId)) {
            throw new ForbiddenException("Interaction is blocked by privacy rules");
        }
    }

    private void validateDistinctProfiles(UUID first, UUID second) {
        if (first.equals(second)) {
            throw new ConflictException("Operation cannot target same profile");
        }
    }

    private FriendRequestResponseDto toDto(FriendRequestEntity entity) {
        return new FriendRequestResponseDto(
            entity.getId(),
            entity.getSenderId(),
            entity.getReceiverId(),
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    private void publishEvent(String type, Map<String, Object> payload) {
        Map<String, Object> eventPayload = new HashMap<>(payload);
        eventPayload.put("producer", "social-features-service");
        domainEventPublisher.publish(type, eventPayload);
    }

    private Pair sortedPair(UUID first, UUID second) {
        if (first.compareTo(second) < 0) {
            return new Pair(first, second);
        }
        return new Pair(second, first);
    }

    private record Pair(UUID first, UUID second) {
    }
}
