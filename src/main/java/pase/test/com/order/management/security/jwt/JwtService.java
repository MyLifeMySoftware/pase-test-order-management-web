package pase.test.com.order.management.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.issuer:pase-auth-service}")
    private String issuer;

    /**
     * Extract username from token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract expiration date from token.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract authorities from token.
     */
    @SuppressWarnings("unchecked")
    public List<String> extractAuthorities(String token) {
        return extractClaim(token, claims -> (List<String>) claims.get("authorities"));
    }

    /**
     * Extract roles from token.
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return extractClaim(token, claims -> (List<String>) claims.get("roles"));
    }

    /**
     * Extract permissions from token.
     */
    @SuppressWarnings("unchecked")
    public List<String> extractPermissions(String token) {
        return extractClaim(token, claims -> (List<String>) claims.get("permissions"));
    }

    /**
     * Extract token type.
     */
    public String extractTokenType(String token) {
        return extractClaim(token, claims -> (String) claims.get("type"));
    }

    /**
     * Extract issuer from token.
     */
    public String extractIssuer(String token) {
        return extractClaim(token, Claims::getIssuer);
    }

    /**
     * Extract claim from token.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract all claims from token.
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            log.error("Error parsing JWT token: {}", e.getMessage());
            throw new JwtException("Invalid JWT token", e);
        }
    }

    /**
     * Check if token is valid (not expired and from correct issuer).
     */
    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);

            // Check if token is expired
            if (claims.getExpiration().before(new Date())) {
                log.debug("Token is expired");
                return false;
            }

            // Check issuer
            if (!issuer.equals(claims.getIssuer())) {
                log.debug("Token issuer mismatch. Expected: {}, Got: {}", issuer, claims.getIssuer());
                return false;
            }

            // Check if it's an access token
            if (!"ACCESS".equals(claims.get("type"))) {
                log.debug("Token is not an access token");
                return false;
            }

            return true;
        } catch (JwtException e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if token is expired.
     */
    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (JwtException e) {
            log.error("Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Check if token is access token.
     */
    public boolean isAccessToken(String token) {
        try {
            return "ACCESS".equals(extractTokenType(token));
        } catch (JwtException e) {
            log.error("Error checking token type: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get signing key.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Validate token format and structure.
     */
    public boolean validateTokenStructure(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            log.debug("Token structure validation failed: {}", e.getMessage());
            return false;
        }
    }
}