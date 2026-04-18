package com.thesis.social.friend.repository;

import com.thesis.social.friend.entity.ProfileBlockEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileBlockRepository extends JpaRepository<ProfileBlockEntity, UUID> {

    boolean existsByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);

    Optional<ProfileBlockEntity> findByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);
}
