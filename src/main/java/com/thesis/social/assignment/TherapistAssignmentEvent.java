package com.thesis.social.assignment;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;

/**
 * Payload published by therapist-api on routing key {@code therapist.assignment.changed}.
 *
 * <p>Parsed field-by-field from the JSON tree (rather than via data-binding) so parsing never
 * depends on a particular Jackson module being registered. Unknown fields are ignored; the three
 * fields below are required, and a message missing any of them is treated as malformed and
 * dead-lettered.
 */
public record TherapistAssignmentEvent(
        UUID therapistProfileId,
        UUID patientProfileId,
        String status) {

    public boolean isValid() {
        return therapistProfileId != null
                && patientProfileId != null
                && status != null;
    }

    /** Parses an event from its JSON body. Returns {@code null} fields for absent/unparseable keys. */
    public static TherapistAssignmentEvent fromJson(JsonNode node) {
        return new TherapistAssignmentEvent(
                uuid(node, "therapistProfileId"),
                uuid(node, "patientProfileId"),
                text(node, "status"));
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static UUID uuid(JsonNode node, String field) {
        String raw = text(node, field);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
