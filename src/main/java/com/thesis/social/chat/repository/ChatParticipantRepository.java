package com.thesis.social.chat.repository;

import com.thesis.social.chat.entity.ChatParticipantEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipantEntity, UUID> {

    List<ChatParticipantEntity> findByProfileId(UUID profileId);

    List<ChatParticipantEntity> findByChannelId(UUID channelId);

    boolean existsByChannelIdAndProfileId(UUID channelId, UUID profileId);
}
