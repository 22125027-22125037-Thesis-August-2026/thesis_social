package com.thesis.social.profile.repository;

import com.thesis.social.profile.entity.ProfileDirectoryEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileDirectoryRepository extends JpaRepository<ProfileDirectoryEntity, UUID> {
}
