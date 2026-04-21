package com.thesis.social.friend.repository;

import com.thesis.social.friend.entity.FriendRequestEntity;
import com.thesis.social.friend.entity.FriendRequestStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FriendRequestRepository extends JpaRepository<FriendRequestEntity, UUID> {

    boolean existsBySenderIdAndReceiverIdAndStatus(UUID senderId, UUID receiverId, FriendRequestStatus status);

    Optional<FriendRequestEntity> findByIdAndStatus(UUID id, FriendRequestStatus status);

    Page<FriendRequestEntity> findByReceiverIdAndStatusOrderByCreatedAtDesc(
        UUID receiverId,
        FriendRequestStatus status,
        Pageable pageable
    );

    Page<FriendRequestEntity> findBySenderIdAndStatusOrderByCreatedAtDesc(
        UUID senderId,
        FriendRequestStatus status,
        Pageable pageable
    );
}
