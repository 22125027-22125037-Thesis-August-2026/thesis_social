package com.thesis.social.chat.repository;

import com.thesis.social.chat.entity.ChatChannelEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatChannelRepository extends JpaRepository<ChatChannelEntity, UUID> {
}
