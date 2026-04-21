package com.thesis.social.security;

import com.thesis.social.common.exception.UnauthorizedException;
import com.thesis.social.config.SocialProperties;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class JwtTokenService {

    private static final String PROFILE_ID_CLAIM = "profile_id";
    private static final String PROFILE_ID_CAMEL_CLAIM = "profileId";
    private static final String ROLES_CLAIM = "roles";
    private static final String ROLE_CLAIM = "role";

    private final SocialProperties socialProperties;
    private final JwtDecoder jwtDecoder;

    public JwtTokenService(SocialProperties socialProperties) {
        this.socialProperties = socialProperties;
        RSAPublicKey publicKey = resolveRsaPublicKey(socialProperties.getSecurity().getJwt().getPublicKey());
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey)
            .signatureAlgorithm(SignatureAlgorithm.RS256)
            .build();
        decoder.setJwtValidator(JwtValidators.createDefault());
        this.jwtDecoder = decoder;
    }

    public AuthenticatedProfile validateAndExtract(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            validateClaims(jwt);
            UUID profileId = resolveProfileId(jwt);
            Set<GrantedAuthority> authorities = parseAuthorities(jwt);
            return new AuthenticatedProfile(profileId, authorities);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new UnauthorizedException("Invalid JWT token");
        }
    }

    private void validateClaims(Jwt jwt) {
        validateKidIfConfigured(jwt);

        String expectedIssuer = socialProperties.getSecurity().getJwt().getIssuer();
        if (StringUtils.hasText(expectedIssuer)) {
            String issuer = jwt.getClaimAsString(JwtClaimNames.ISS);
            if (issuer == null || !expectedIssuer.equals(issuer)) {
                throw new UnauthorizedException("Invalid token issuer");
            }
        }

        String expectedAudience = socialProperties.getSecurity().getJwt().getAudience();
        if (StringUtils.hasText(expectedAudience)) {
            List<String> audience = readAudienceClaim(jwt.getClaims().get(JwtClaimNames.AUD));
            if (audience == null || !audience.contains(expectedAudience)) {
                throw new UnauthorizedException("Invalid token audience");
            }
        }

        Instant expiresAt = jwt.getExpiresAt();
        if (expiresAt != null && expiresAt.isBefore(Instant.now())) {
            throw new UnauthorizedException("Token expired");
        }

        if (resolveProfileIdRaw(jwt) == null) {
            throw new UnauthorizedException("Missing profile claim");
        }
    }

    private UUID resolveProfileId(Jwt jwt) {
        Object rawProfileId = resolveProfileIdRaw(jwt);
        return UUID.fromString(rawProfileId.toString());
    }

    private Object resolveProfileIdRaw(Jwt jwt) {
        Object snakeCaseProfileId = jwt.getClaim(PROFILE_ID_CLAIM);
        if (snakeCaseProfileId != null) {
            return snakeCaseProfileId;
        }
        return jwt.getClaim(PROFILE_ID_CAMEL_CLAIM);
    }

    private Set<GrantedAuthority> parseAuthorities(Jwt jwt) {
        Object claim = jwt.getClaim(ROLES_CLAIM);
        if (claim == null) {
            claim = jwt.getClaim(ROLE_CLAIM);
        }

        Set<GrantedAuthority> authorities = new HashSet<>();
        if (claim instanceof List<?> roleList) {
            for (Object role : roleList) {
                addAuthoritiesForRoleValue(authorities, String.valueOf(role));
            }
        } else if (claim instanceof String role) {
            addAuthoritiesForRoleValue(authorities, role);
        }

        // Any valid JWT principal is at least an authenticated user in this service.
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        return authorities;
    }

    private void addAuthoritiesForRoleValue(Set<GrantedAuthority> authorities, String roleValue) {
        if (!StringUtils.hasText(roleValue)) {
            return;
        }

        authorities.add(new SimpleGrantedAuthority(roleValue));

        if (!roleValue.startsWith("ROLE_")) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + roleValue));
        }
    }

    private List<String> readAudienceClaim(Object audClaim) {
        if (audClaim instanceof String aud) {
            return List.of(aud);
        }
        if (audClaim instanceof List<?> audList) {
            List<String> converted = new ArrayList<>();
            for (Object value : audList) {
                converted.add(String.valueOf(value));
            }
            return converted;
        }
        return null;
    }

    private void validateKidIfConfigured(Jwt jwt) {
        String expectedKid = socialProperties.getSecurity().getJwt().getSigningKid();
        if (!StringUtils.hasText(expectedKid)) {
            return;
        }

        Object rawKid = jwt.getHeaders().get("kid");
        if (rawKid == null || !expectedKid.equals(rawKid.toString())) {
            throw new UnauthorizedException("Invalid token signing key id");
        }
    }

    private RSAPublicKey resolveRsaPublicKey(String configuredPublicKey) {
        if (!StringUtils.hasText(configuredPublicKey)) {
            throw new IllegalStateException(
                "Missing JWT verification key: set SOCIAL_JWT_PUBLIC_KEY (Base64 X.509 RSA public key)"
            );
        }

        try {
            String normalized = configuredPublicKey.replaceAll("\\s+", "");
            byte[] keyBytes = Base64.getDecoder().decode(normalized);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(keySpec);
            if (!(publicKey instanceof RSAPublicKey rsaPublicKey)) {
                throw new IllegalStateException("Configured key is not an RSA public key");
            }
            return rsaPublicKey;
        } catch (IllegalArgumentException | GeneralSecurityException ex) {
            throw new IllegalStateException(
                "Invalid SOCIAL_JWT_PUBLIC_KEY: expected Base64 X.509 RSA public key",
                ex
            );
        }
    }
}
