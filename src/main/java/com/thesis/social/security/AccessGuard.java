package com.thesis.social.security;

import java.security.Principal;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("accessGuard")
public class AccessGuard {

    public boolean isSelfOrAdmin(UUID profileId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return false;
        }

        if (authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch("ROLE_ADMIN"::equals)) {
            return true;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedProfile authenticatedProfile) {
            return authenticatedProfile.getProfileId().equals(profileId);
        }

        return false;
    }

    public UUID extractProfileId(Principal principal) {
        if (principal instanceof AuthenticatedProfile authenticatedProfile) {
            return authenticatedProfile.getProfileId();
        }

        throw new IllegalStateException("Invalid authenticated principal");
    }
}
