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
    private String expectedIssuer;

    /**
     * Extract username from token.
     */
    public String extractUsername(String token) {
        try {
            return extractClaim(token, Claims::getSubject);
        } catch (Exception e) {
            log.error("Error extracting username from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract expiration date from token.
     */
    public Date extractExpiration(String token) {
        try {
            return extractClaim(token, Claims::getExpiration);
        } catch (Exception e) {
            log.error("Error extracting expiration from token: {}", e.getMessage());
            return new Date(0); // Return past date
        }
    }

    /**
     * Extract authorities from token.
     */
    @SuppressWarnings("unchecked")
    public List<String> extractAuthorities(String token) {
        try {
            return extractClaim(token, claims -> (List<String>) claims.get("authorities"));
        } catch (Exception e) {
            log.error("Error extracting authorities from token: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Extract roles from token.
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        try {
            return extractClaim(token, claims -> (List<String>) claims.get("roles"));
        } catch (Exception e) {
            log.error("Error extracting roles from token: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Extract permissions from token.
     */
    @SuppressWarnings("unchecked")
    public List<String> extractPermissions(String token) {
        try {
            return extractClaim(token, claims -> (List<String>) claims.get("permissions"));
        } catch (Exception e) {
            log.error("Error extracting permissions from token: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Extract token type.
     */
    public String extractTokenType(String token) {
        try {
            return extractClaim(token, claims -> (String) claims.get("type"));
        } catch (Exception e) {
            log.error("Error extracting token type: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract issuer from token.
     */
    public String extractIssuer(String token) {
        try {
            return extractClaim(token, Claims::getIssuer);
        } catch (Exception e) {
            log.error("Error extracting issuer from token: {}", e.getMessage());
            return null;
        }
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

            // Check expiration
            Date expiration = claims.getExpiration();
            if (expiration != null && expiration.before(new Date())) {
                log.debug("Token is expired. Expiration: {}, Current: {}", expiration, new Date());
                return false;
            }

            // Check issuer
            String issuer = claims.getIssuer();
            if (expectedIssuer != null && !expectedIssuer.equals(issuer)) {
                log.debug("Token issuer mismatch. Expected: {}, Got: {}", expectedIssuer, issuer);
                return false;
            }

            // Check token type
            String tokenType = (String) claims.get("type");
            if (!"ACCESS".equals(tokenType)) {
                log.debug("Token is not an access token. Type: {}", tokenType);
                return false;
            }

            // Check subject (username)
            String subject = claims.getSubject();
            if (subject == null || subject.trim().isEmpty()) {
                log.debug("Token has no subject (username)");
                return false;
            }

            log.debug("Token validation successful for user: {}", subject);
            return true;

        } catch (JwtException e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error during token validation: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if token is expired.
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = extractExpiration(token);
            boolean expired = expiration.before(new Date());
            if (expired) {
                log.debug("Token is expired. Expiration: {}, Current: {}", expiration, new Date());
            }
            return expired;
        } catch (Exception e) {
            log.error("Error checking token expiration: {}", e.getMessage());
            return true; // Assume expired if we can't check
        }
    }

    /**
     * Check if token is access token.
     */
    public boolean isAccessToken(String token) {
        try {
            String tokenType = extractTokenType(token);
            boolean isAccess = "ACCESS".equals(tokenType);
            if (!isAccess) {
                log.debug("Token is not an access token. Type: {}", tokenType);
            }
            return isAccess;
        } catch (Exception e) {
            log.error("Error checking token type: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get signing key.
     */
    private SecretKey getSigningKey() {
        try {
            byte[] keyBytes = secretKey.getBytes();
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            log.error("Error creating signing key: {}", e.getMessage());
            throw new RuntimeException("Failed to create JWT signing key", e);
        }
    }

    /**
     * Validate token format and structure.
     */
    public boolean validateTokenStructure(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                log.debug("Token is null or empty");
                return false;
            }

            // Check basic JWT structure (3 parts separated by dots)
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                log.debug("Token doesn't have 3 parts. Parts: {}", parts.length);
                return false;
            }

            // Try to parse the token
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);

            return true;
        } catch (JwtException e) {
            log.debug("Token structure validation failed: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error during token structure validation: {}", e.getMessage());
            return false;
        }
    }
}