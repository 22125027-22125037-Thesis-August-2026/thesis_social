package com.thesis.social.security;

import com.thesis.social.common.exception.UnauthorizedException;
import java.security.Principal;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class PrincipalAccess {

    public UUID currentProfileId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedProfile profile)) {
            throw new UnauthorizedException("Unauthenticated request");
        }
        return profile.getProfileId();
    }

    public UUID profileIdFromPrincipal(Principal principal) {
        if (principal instanceof AuthenticatedProfile profile) {
            return profile.getProfileId();
        }
        throw new UnauthorizedException("Invalid websocket principal");
    }
}
