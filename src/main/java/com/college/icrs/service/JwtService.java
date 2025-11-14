package com.college.icrs.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${security.jwt.secret-key}")
    private String secretKey;

    @Value("${security.jwt.expiration-time}")
    private long jwtExpiration; // e.g. 86400000 = 24h

    /**
     * ✅ Extracts the JWT subject (email) from the token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Generic claim extractor.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * ✅ Always uses user email as JWT subject, even if getUsername() returns full name.
     */
    public String generateToken(UserDetails userDetails) {
        String subject = resolveSubject(userDetails);
        return buildToken(Map.of(), subject, jwtExpiration);
    }

    /**
     * ✅ Overloaded token builder that supports extra claims.
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        String subject = resolveSubject(userDetails);
        return buildToken(extraClaims, subject, jwtExpiration);
    }

    /**
     * ✅ Determines what goes inside the JWT `sub` (subject) field.
     * Always email-based to keep downstream logic consistent.
     */
    private String resolveSubject(UserDetails userDetails) {
        if (userDetails instanceof com.college.icrs.model.User user) {
            // Always use the email field for token identity
            return user.getEmail();
        }
        // Fallback (should never happen unless another UserDetails type is used)
        return userDetails.getUsername();
    }

    /**
     * ✅ Builds and signs the JWT.
     */
    private String buildToken(Map<String, Object> extraClaims, String subject, long expiration) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiration))
                .signWith(getSignInKey())
                .compact();
    }

    /**
     * ✅ Validates token (checks subject and expiry).
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String email = extractUsername(token);
        final boolean notExpired = !isTokenExpired(token);

        if (userDetails instanceof com.college.icrs.model.User user) {
            return email.equalsIgnoreCase(user.getEmail()) && notExpired;
        }
        return email.equalsIgnoreCase(userDetails.getUsername()) && notExpired;
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * ✅ Extracts all JWT claims using the configured signing key.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * ✅ Creates the signing key from Base64-encoded secret.
     */
    private SecretKey getSignInKey() {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secretKey);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid or missing JWT secret key configuration.", e);
        }
    }

    public long getExpirationTime() {
        return jwtExpiration;
    }
}
