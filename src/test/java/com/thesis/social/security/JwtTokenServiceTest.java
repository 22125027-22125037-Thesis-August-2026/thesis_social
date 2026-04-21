package com.thesis.social.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.thesis.social.common.exception.UnauthorizedException;
import com.thesis.social.config.SocialProperties;
import com.thesis.social.support.JwtTestTokenFactory;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtTokenServiceTest {

    private static final String ISSUER = "therapist-auth";
    private static final String AUDIENCE = "social-features";
    private static final String SIGNING_KID = "therapist-rsa-key-1";

    private JwtTokenService jwtTokenService;
    private JwtTestTokenFactory.KeyMaterial keyMaterial;

    @BeforeEach
    void setUp() {
        keyMaterial = JwtTestTokenFactory.generateRsaKeyMaterial();
        SocialProperties properties = new SocialProperties();
        properties.getSecurity().getJwt().setPublicKey(keyMaterial.getPublicKeyBase64());
        properties.getSecurity().getJwt().setSigningKid(SIGNING_KID);
        properties.getSecurity().getJwt().setIssuer(ISSUER);
        properties.getSecurity().getJwt().setAudience(AUDIENCE);
        jwtTokenService = new JwtTokenService(properties);
    }

    @Test
    void shouldValidateJwtAndExtractProfile() {
        UUID profileId = UUID.randomUUID();
        String token = JwtTestTokenFactory.createToken(
            keyMaterial.getPrivateKey(),
            ISSUER,
            AUDIENCE,
            profileId,
            List.of("ROLE_USER"),
            SIGNING_KID,
            Instant.now().plusSeconds(3600)
        );

        AuthenticatedProfile profile = jwtTokenService.validateAndExtract(token);

        assertEquals(profileId, profile.getProfileId());
        assertEquals("ROLE_USER", profile.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void shouldAcceptCamelCaseProfileIdClaim() {
        UUID profileId = UUID.randomUUID();
        String token = JwtTestTokenFactory.createToken(
            keyMaterial.getPrivateKey(),
            ISSUER,
            AUDIENCE,
            profileId,
            "profileId",
            List.of("ROLE_USER"),
            "roles",
            SIGNING_KID,
            Instant.now().plusSeconds(3600)
        );

        AuthenticatedProfile profile = jwtTokenService.validateAndExtract(token);

        assertEquals(profileId, profile.getProfileId());
    }

    @Test
    void shouldAcceptSingularRoleClaimWhenRolesClaimMissing() {
        UUID profileId = UUID.randomUUID();
        String token = JwtTestTokenFactory.createToken(
            keyMaterial.getPrivateKey(),
            ISSUER,
            AUDIENCE,
            profileId,
            "profileId",
            "TEEN",
            "role",
            SIGNING_KID,
            Instant.now().plusSeconds(3600)
        );

        AuthenticatedProfile profile = jwtTokenService.validateAndExtract(token);

        assertEquals(profileId, profile.getProfileId());
        Set<String> authorities = profile.getAuthorities()
            .stream()
            .map(granted -> granted.getAuthority())
            .collect(Collectors.toSet());

        assertTrue(authorities.contains("TEEN"));
        assertTrue(authorities.contains("ROLE_TEEN"));
        assertTrue(authorities.contains("ROLE_USER"));
    }

    @Test
    void shouldRejectTokenWithInvalidSignature() {
        JwtTestTokenFactory.KeyMaterial wrongKey = JwtTestTokenFactory.generateRsaKeyMaterial();
        String token = JwtTestTokenFactory.createToken(
            wrongKey.getPrivateKey(),
            ISSUER,
            AUDIENCE,
            UUID.randomUUID(),
            List.of("ROLE_USER"),
            SIGNING_KID,
            Instant.now().plusSeconds(3600)
        );

        assertThrows(UnauthorizedException.class, () -> jwtTokenService.validateAndExtract(token));
    }

    @Test
    void shouldRejectInvalidIssuer() {
        String token = JwtTestTokenFactory.createToken(
            keyMaterial.getPrivateKey(),
            "wrong-issuer",
            AUDIENCE,
            UUID.randomUUID(),
            List.of("ROLE_USER"),
            SIGNING_KID,
            Instant.now().plusSeconds(3600)
        );

        assertThrows(UnauthorizedException.class, () -> jwtTokenService.validateAndExtract(token));
    }

    @Test
    void shouldRejectInvalidAudienceWhenStringClaim() {
        String token = JwtTestTokenFactory.createToken(
            keyMaterial.getPrivateKey(),
            ISSUER,
            "wrong-audience",
            UUID.randomUUID(),
            List.of("ROLE_USER"),
            SIGNING_KID,
            Instant.now().plusSeconds(3600)
        );

        assertThrows(UnauthorizedException.class, () -> jwtTokenService.validateAndExtract(token));
    }

    @Test
    void shouldRejectInvalidAudienceWhenListClaim() {
        String token = JwtTestTokenFactory.createToken(
            keyMaterial.getPrivateKey(),
            ISSUER,
            List.of("wrong-audience", "also-wrong"),
            UUID.randomUUID(),
            List.of("ROLE_USER"),
            SIGNING_KID,
            Instant.now().plusSeconds(3600)
        );

        assertThrows(UnauthorizedException.class, () -> jwtTokenService.validateAndExtract(token));
    }

    @Test
    void shouldAcceptAudienceWhenListContainsExpectedValue() {
        UUID profileId = UUID.randomUUID();
        String token = JwtTestTokenFactory.createToken(
            keyMaterial.getPrivateKey(),
            ISSUER,
            List.of("other-audience", AUDIENCE),
            profileId,
            List.of("ROLE_USER"),
            SIGNING_KID,
            Instant.now().plusSeconds(3600)
        );

        AuthenticatedProfile profile = jwtTokenService.validateAndExtract(token);

        assertEquals(profileId, profile.getProfileId());
    }

    @Test
    void shouldRejectWrongKidWhenConfigured() {
        String token = JwtTestTokenFactory.createToken(
            keyMaterial.getPrivateKey(),
            ISSUER,
            AUDIENCE,
            UUID.randomUUID(),
            List.of("ROLE_USER"),
            "wrong-kid",
            Instant.now().plusSeconds(3600)
        );

        assertThrows(UnauthorizedException.class, () -> jwtTokenService.validateAndExtract(token));
    }

    @Test
    void shouldAcceptTokenWithoutKidWhenKidNotConfigured() {
        SocialProperties properties = new SocialProperties();
        properties.getSecurity().getJwt().setPublicKey(keyMaterial.getPublicKeyBase64());
        properties.getSecurity().getJwt().setIssuer(ISSUER);
        properties.getSecurity().getJwt().setAudience(AUDIENCE);
        JwtTokenService serviceWithoutKid = new JwtTokenService(properties);

        UUID profileId = UUID.randomUUID();
        String token = JwtTestTokenFactory.createToken(
            keyMaterial.getPrivateKey(),
            ISSUER,
            AUDIENCE,
            profileId,
            List.of("ROLE_USER"),
            null,
            Instant.now().plusSeconds(3600)
        );

        AuthenticatedProfile profile = serviceWithoutKid.validateAndExtract(token);
        assertEquals(profileId, profile.getProfileId());
    }

    @Test
    void shouldRejectExpiredTokenWhenExpClaimPresent() {
        String token = JwtTestTokenFactory.createToken(
            keyMaterial.getPrivateKey(),
            ISSUER,
            AUDIENCE,
            UUID.randomUUID(),
            List.of("ROLE_USER"),
            SIGNING_KID,
            Instant.now().minusSeconds(60)
        );

        assertThrows(UnauthorizedException.class, () -> jwtTokenService.validateAndExtract(token));
    }

    @Test
    void shouldFailFastWhenPublicKeyMissing() {
        SocialProperties properties = new SocialProperties();
        properties.getSecurity().getJwt().setIssuer(ISSUER);
        properties.getSecurity().getJwt().setAudience(AUDIENCE);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> new JwtTokenService(properties));
        assertNotNull(exception.getMessage());
    }

    @Test
    void shouldFailFastWhenPublicKeyIsInvalid() {
        SocialProperties properties = new SocialProperties();
        properties.getSecurity().getJwt().setPublicKey("not-base64");
        properties.getSecurity().getJwt().setIssuer(ISSUER);
        properties.getSecurity().getJwt().setAudience(AUDIENCE);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> new JwtTokenService(properties));
        assertNotNull(exception.getMessage());
    }

    @Test
    void shouldAcceptTokenWithoutExpClaim() {
        UUID profileId = UUID.randomUUID();
        String token = JwtTestTokenFactory.createToken(
            keyMaterial.getPrivateKey(),
            ISSUER,
            AUDIENCE,
            profileId,
            List.of("ROLE_USER"),
            SIGNING_KID,
            null
        );

        AuthenticatedProfile profile = jwtTokenService.validateAndExtract(token);
        assertEquals(profileId, profile.getProfileId());
    }
}
