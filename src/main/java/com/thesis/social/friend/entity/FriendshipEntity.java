package com.thesis.social.friend.entity;

import com.thesis.social.common.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "friendships")
public class FriendshipEntity extends AuditableEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "profile_id_1", nullable = false)
    private UUID profileId1;

    @Column(name = "profile_username_1")
    private String profileUsername1;

    @Column(name = "profile_id_2", nullable = false)
    private UUID profileId2;

    @Column(name = "profile_username_2")
    private String profileUsername2;

    public UUID getId() {
        return id;
    }

    public UUID getProfileId1() {
        return profileId1;
    }

    public void setProfileId1(UUID profileId1) {
        this.profileId1 = profileId1;
    }

    public UUID getProfileId2() {
        return profileId2;
    }

    public void setProfileId2(UUID profileId2) {
        this.profileId2 = profileId2;
    }

    public String getProfileUsername1() {
        return profileUsername1;
    }

    public void setProfileUsername1(String profileUsername1) {
        this.profileUsername1 = profileUsername1;
    }

    public String getProfileUsername2() {
        return profileUsername2;
    }

    public void setProfileUsername2(String profileUsername2) {
        this.profileUsername2 = profileUsername2;
    }
}
