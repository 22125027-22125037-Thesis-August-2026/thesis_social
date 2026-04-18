package com.thesis.social.security;

import java.security.Principal;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;

public class AuthenticatedProfile implements Principal {

    private final UUID profileId;
    private final Set<GrantedAuthority> authorities;

    public AuthenticatedProfile(UUID profileId, Set<GrantedAuthority> authorities) {
        this.profileId = profileId;
        this.authorities = authorities;
    }

    public UUID getProfileId() {
        return profileId;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getName() {
        return profileId.toString();
    }
}
