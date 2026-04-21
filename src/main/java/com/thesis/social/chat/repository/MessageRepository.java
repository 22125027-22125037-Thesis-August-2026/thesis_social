package com.thesis.social.chat.repository;

import com.thesis.social.chat.entity.MessageEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<MessageEntity, UUID> {

    Page<MessageEntity> findByChannelIdOrderByCreatedAtDesc(UUID channelId, Pageable pageable);

    Optional<MessageEntity> findByIdAndChannelId(UUID id, UUID channelId);

    Optional<MessageEntity> findFirstByChannelIdOrderByCreatedAtDesc(UUID channelId);

    long countByChannelIdAndIsReadFalseAndSenderIdNot(UUID channelId, UUID senderId);
}
