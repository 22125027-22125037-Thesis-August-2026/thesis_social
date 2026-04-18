package com.thesis.social.chat.entity;

import com.thesis.social.common.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "chat_channels")
public class ChatChannelEntity extends AuditableEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ChatChannelType type;

    @Column(name = "reference_id")
    private UUID referenceId;

    public UUID getId() {
        return id;
    }

    public ChatChannelType getType() {
        return type;
    }

    public void setType(ChatChannelType type) {
        this.type = type;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(UUID referenceId) {
        this.referenceId = referenceId;
    }
}
