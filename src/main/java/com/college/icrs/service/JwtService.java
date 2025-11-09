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
    private long jwtExpiration;

    /**
     * Extract username (email) from JWT token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract a specific claim from the JWT
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * ✅ Generate a JWT using the user's email as the subject.
     * This is the main method your AuthenticationService should use.
     */
    public String generateToken(UserDetails userDetails) {
        String subject;

        // Ensure the email is stored in the "sub" claim
        if (userDetails instanceof com.college.icrs.model.User user) {
            subject = user.getEmail();
        } else {
            subject = userDetails.getUsername(); // fallback
        }

        return buildToken(Map.of(), subject, jwtExpiration);
    }

    /**
     * Overloaded version to include custom claims if needed.
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        String subject;

        if (userDetails instanceof com.college.icrs.model.User user) {
            subject = user.getEmail();
        } else {
            subject = userDetails.getUsername();
        }

        return buildToken(extraClaims, subject, jwtExpiration);
    }

    /**
     * Build the actual JWT token with claims, subject, issue time, and expiry.
     */
    private String buildToken(Map<String, Object> extraClaims, String subject, long expiration) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey())
                .compact();
    }

    /**
     * Validate a JWT token by comparing the email and ensuring it’s not expired.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String email = extractUsername(token);
        if (userDetails instanceof com.college.icrs.model.User user) {
            return email.equals(user.getEmail()) && !isTokenExpired(token);
        }
        return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /**
     * Check if the token has expired.
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract all claims safely.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Generate the signing key from the base64-encoded secret.
     */
    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Return the configured expiration time (for frontend usage).
     */
    public long getExpirationTime() {
        return jwtExpiration;
    }
}
