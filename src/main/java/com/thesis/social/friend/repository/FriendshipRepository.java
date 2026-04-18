package com.thesis.social.friend.repository;

import com.thesis.social.friend.entity.FriendshipEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FriendshipRepository extends JpaRepository<FriendshipEntity, UUID> {

    boolean existsByProfileId1AndProfileId2(UUID profileId1, UUID profileId2);

    Optional<FriendshipEntity> findByProfileId1AndProfileId2(UUID profileId1, UUID profileId2);

    List<FriendshipEntity> findByProfileId1OrProfileId2(UUID profileId1, UUID profileId2);
}
