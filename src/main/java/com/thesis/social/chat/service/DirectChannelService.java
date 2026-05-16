package com.thesis.social.chat.service;

import com.thesis.social.chat.entity.ChatChannelEntity;
import com.thesis.social.chat.entity.ChatChannelType;
import com.thesis.social.chat.entity.ChatParticipantEntity;
import com.thesis.social.chat.repository.ChatChannelRepository;
import com.thesis.social.chat.repository.ChatParticipantRepository;
import com.thesis.social.friend.entity.FriendshipEntity;
import com.thesis.social.friend.repository.FriendshipRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DirectChannelService {

    private final ChatChannelRepository chatChannelRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final FriendshipRepository friendshipRepository;

    public DirectChannelService(ChatChannelRepository chatChannelRepository,
                                ChatParticipantRepository chatParticipantRepository,
                                FriendshipRepository friendshipRepository) {
        this.chatChannelRepository = chatChannelRepository;
        this.chatParticipantRepository = chatParticipantRepository;
        this.friendshipRepository = friendshipRepository;
    }

    @Transactional
    public ChatChannelEntity ensureDirectFriendChannel(UUID profileA, UUID profileB) {
        List<ChatChannelEntity> existing = chatChannelRepository.findDirectFriendChannelsBetween(profileA, profileB);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }

        ChatChannelEntity channel = new ChatChannelEntity();
        channel.setType(ChatChannelType.DIRECT_FRIEND);
        channel.setReferenceId(resolveFriendshipReferenceId(profileA, profileB));
        ChatChannelEntity savedChannel = chatChannelRepository.save(channel);

        addParticipant(savedChannel.getId(), profileA);
        addParticipant(savedChannel.getId(), profileB);

        return savedChannel;
    }

    @Transactional
    public void removeDirectFriendChannel(UUID profileA, UUID profileB) {
        List<ChatChannelEntity> channels = chatChannelRepository.findDirectFriendChannelsBetween(profileA, profileB);
        if (!channels.isEmpty()) {
            chatChannelRepository.deleteAll(channels);
        }
    }

    private UUID resolveFriendshipReferenceId(UUID profileA, UUID profileB) {
        UUID first = profileA.compareTo(profileB) < 0 ? profileA : profileB;
        UUID second = profileA.compareTo(profileB) < 0 ? profileB : profileA;
        return friendshipRepository.findByProfileId1AndProfileId2(first, second)
            .map(FriendshipEntity::getId)
            .orElse(null);
    }

    private void addParticipant(UUID channelId, UUID profileId) {
        ChatParticipantEntity participant = new ChatParticipantEntity();
        participant.setChannelId(channelId);
        participant.setProfileId(profileId);
        chatParticipantRepository.save(participant);
    }
}
