package com.thesis.social.profile.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "profile_directory")
public class ProfileDirectoryEntity {

    @Id
    @Column(name = "profile_id", nullable = false)
    private UUID profileId;

    @Column(name = "profile_name", nullable = false)
    private String profileName;

    public UUID getProfileId() {
        return profileId;
    }

    public String getProfileName() {
        return profileName;
    }
}
