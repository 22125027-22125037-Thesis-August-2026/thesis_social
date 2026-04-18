package com.thesis.social.security;

import com.thesis.social.common.exception.UnauthorizedException;
import com.thesis.social.config.SocialProperties;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private static final String PROFILE_ID_CLAIM = "profile_id";
    private static final String ROLES_CLAIM = "roles";

    private final SocialProperties socialProperties;
    private final JwtDecoder jwtDecoder;

    public JwtTokenService(SocialProperties socialProperties) {
        this.socialProperties = socialProperties;
        byte[] secret = socialProperties.getSecurity().getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(new SecretKeySpec(secret, "HmacSHA256"))
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
        decoder.setJwtValidator(JwtValidators.createDefault());
        this.jwtDecoder = decoder;
    }

    public AuthenticatedProfile validateAndExtract(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            JwtClaimsSet claimsSet = JwtClaimsSet.builder().claims(claims -> claims.putAll(jwt.getClaims())).build();
            validateClaims(claimsSet);
            UUID profileId = UUID.fromString(claimsSet.getClaim(PROFILE_ID_CLAIM).toString());
            Set<GrantedAuthority> authorities = parseAuthorities(claimsSet.getClaim(ROLES_CLAIM));
            return new AuthenticatedProfile(profileId, authorities);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new UnauthorizedException("Invalid JWT token");
        }
    }

    private void validateClaims(JwtClaimsSet claimsSet) {
        String expectedIssuer = socialProperties.getSecurity().getJwt().getIssuer();
        String expectedAudience = socialProperties.getSecurity().getJwt().getAudience();

        if (claimsSet.getIssuer() == null || !expectedIssuer.equals(claimsSet.getIssuer().toString())) {
            throw new UnauthorizedException("Invalid token issuer");
        }

        List<String> audience = claimsSet.getAudience();
        if (audience == null || !audience.contains(expectedAudience)) {
            throw new UnauthorizedException("Invalid token audience");
        }

        Instant expiresAt = claimsSet.getExpiresAt();
        if (expiresAt == null || expiresAt.isBefore(Instant.now())) {
            throw new UnauthorizedException("Token expired");
        }

        if (claimsSet.getClaim(PROFILE_ID_CLAIM) == null) {
            throw new UnauthorizedException("Missing profile claim");
        }
    }

    private Set<GrantedAuthority> parseAuthorities(Object claim) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        if (claim instanceof List<?> roleList) {
            for (Object role : roleList) {
                authorities.add(new SimpleGrantedAuthority(String.valueOf(role)));
            }
        }
        if (authorities.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }
        return authorities;
    }
}
