package com.thesis.social.support;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public final class JwtTestTokenFactory {

    public static final class KeyMaterial {
        private final RSAPrivateKey privateKey;
        private final String publicKeyBase64;

        public KeyMaterial(RSAPrivateKey privateKey, String publicKeyBase64) {
            this.privateKey = privateKey;
            this.publicKeyBase64 = publicKeyBase64;
        }

        public RSAPrivateKey getPrivateKey() {
            return privateKey;
        }

        public String getPublicKeyBase64() {
            return publicKeyBase64;
        }
    }

    private JwtTestTokenFactory() {
    }

    public static KeyMaterial generateRsaKeyMaterial() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
            String base64PublicKey = Base64.getEncoder().encodeToString(publicKey.getEncoded());

            return new KeyMaterial(privateKey, base64PublicKey);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Failed to generate RSA key pair for test", ex);
        }
    }

    public static String createToken(
        RSAPrivateKey privateKey,
        String issuer,
        Object audience,
        UUID profileId,
        List<String> roles,
        String kid,
        Instant expiration
    ) {
        return createToken(
            privateKey,
            issuer,
            audience,
            profileId,
            "profile_id",
            roles,
            "roles",
            kid,
            expiration
        );
    }

    public static String createToken(
        RSAPrivateKey privateKey,
        String issuer,
        Object audience,
        UUID profileId,
        String profileClaimName,
        Object rolesClaimValue,
        String rolesClaimName,
        String kid,
        Instant expiration
    ) {
        try {
            Instant now = Instant.now();
            JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(profileId.toString())
                .issueTime(Date.from(now))
                .claim(profileClaimName, profileId.toString())
                .claim(rolesClaimName, rolesClaimValue);

            if (audience instanceof String audienceString) {
                claimsBuilder.audience(audienceString);
            } else if (audience instanceof List<?> audienceList) {
                claimsBuilder.claim("aud", audienceList);
            }

            if (expiration != null) {
                claimsBuilder.expirationTime(Date.from(expiration));
            }

            JWTClaimsSet claims = claimsBuilder.build();

            JWSHeader.Builder headerBuilder = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(JOSEObjectType.JWT);
            if (kid != null) {
                headerBuilder.keyID(kid);
            }

            SignedJWT jwt = new SignedJWT(headerBuilder.build(), claims);
            jwt.sign(new RSASSASigner(privateKey));
            return jwt.serialize();
        } catch (JOSEException ex) {
            throw new IllegalStateException("Failed to create JWT token for test", ex);
        }
    }
}
