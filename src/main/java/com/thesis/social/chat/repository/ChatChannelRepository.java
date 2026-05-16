package com.thesis.social.chat.repository;

import com.thesis.social.chat.entity.ChatChannelEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatChannelRepository extends JpaRepository<ChatChannelEntity, UUID> {

    @Query("SELECT c FROM ChatChannelEntity c "
        + "WHERE c.type = com.thesis.social.chat.entity.ChatChannelType.DIRECT_FRIEND "
        + "AND EXISTS (SELECT 1 FROM ChatParticipantEntity p1 WHERE p1.channelId = c.id AND p1.profileId = :profileA) "
        + "AND EXISTS (SELECT 1 FROM ChatParticipantEntity p2 WHERE p2.channelId = c.id AND p2.profileId = :profileB)")
    List<ChatChannelEntity> findDirectFriendChannelsBetween(@Param("profileA") UUID profileA,
                                                            @Param("profileB") UUID profileB);
}
