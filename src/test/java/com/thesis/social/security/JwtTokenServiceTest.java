package com.thesis.social.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.thesis.social.common.exception.UnauthorizedException;
import com.thesis.social.config.SocialProperties;
import com.thesis.social.support.JwtTestTokenFactory;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtTokenServiceTest {

    private static final String SECRET = "dev-secret-change-me-dev-secret-change-me";
    private static final String ISSUER = "therapist-auth";
    private static final String AUDIENCE = "social-features";

    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        SocialProperties properties = new SocialProperties();
        properties.getSecurity().getJwt().setSecret(SECRET);
        properties.getSecurity().getJwt().setIssuer(ISSUER);
        properties.getSecurity().getJwt().setAudience(AUDIENCE);
        jwtTokenService = new JwtTokenService(properties);
    }

    @Test
    void shouldValidateJwtAndExtractProfile() {
        UUID profileId = UUID.randomUUID();
        String token = JwtTestTokenFactory.createToken(SECRET, ISSUER, AUDIENCE, profileId, List.of("ROLE_USER"));

        AuthenticatedProfile profile = jwtTokenService.validateAndExtract(token);

        assertEquals(profileId, profile.getProfileId());
        assertEquals("ROLE_USER", profile.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void shouldRejectInvalidIssuer() {
        String token = JwtTestTokenFactory.createToken(SECRET, "wrong", AUDIENCE, UUID.randomUUID(), List.of("ROLE_USER"));
        assertThrows(UnauthorizedException.class, () -> jwtTokenService.validateAndExtract(token));
    }
}
