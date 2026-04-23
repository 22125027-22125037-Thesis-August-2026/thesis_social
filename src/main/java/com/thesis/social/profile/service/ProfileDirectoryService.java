package com.thesis.social.profile.service;

import com.thesis.social.profile.repository.ProfileDirectoryRepository;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileDirectoryService {

    private final ProfileDirectoryRepository profileDirectoryRepository;

    public ProfileDirectoryService(ProfileDirectoryRepository profileDirectoryRepository) {
        this.profileDirectoryRepository = profileDirectoryRepository;
    }

    @Transactional(readOnly = true)
    public String resolveProfileName(UUID profileId) {
        return profileDirectoryRepository.findById(profileId)
            .map(entity -> entity.getProfileName())
            .orElse("Unknown User");
    }

    @Transactional(readOnly = true)
    public Map<UUID, String> resolveProfileNames(Set<UUID> profileIds) {
        if (profileIds == null || profileIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return profileDirectoryRepository.findAllById(profileIds).stream()
            .collect(Collectors.toMap(
                entity -> entity.getProfileId(),
                entity -> entity.getProfileName()
            ));
    }
}
