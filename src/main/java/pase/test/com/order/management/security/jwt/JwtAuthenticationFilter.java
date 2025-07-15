package pase.test.com.order.management.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String secretKey;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Check if this request should be filtered
        if (shouldNotFilter(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // Check if Authorization header is present and starts with Bearer
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No Authorization header found or doesn't start with Bearer");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Extract JWT token
            jwt = authHeader.substring(7);
            log.debug("JWT token extracted: {}", jwt.substring(0, Math.min(jwt.length(), 20)) + "...");

            // Validate token structure and extract username
            username = extractUsername(jwt);
            log.debug("Username extracted from token: {}", username);

            // Check if user is not already authenticated
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Validate token
                if (isTokenValid(jwt)) {
                    log.debug("Token is valid for user: {}", username);

                    // Extract authorities from token
                    List<String> tokenAuthorities = extractAuthorities(jwt);
                    List<SimpleGrantedAuthority> authorities = tokenAuthorities.stream()
                            .map(SimpleGrantedAuthority::new)
                            .toList();

                    log.debug("Authorities extracted: {}", authorities);

                    // Create authentication token
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            authorities
                    );

                    // Set authentication details
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Set authentication in security context
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    log.info("User {} authenticated successfully via JWT with authorities: {}",
                            username, authorities);
                } else {
                    log.warn("JWT token is invalid for user: {}", username);
                }
            }
        } catch (JwtException e) {
            log.error("JWT token validation failed: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Error processing JWT token: {}", e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // Skip JWT validation for public endpoints
        boolean isPublicEndpoint = path.startsWith("/actuator") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/api/v1/orders/health") ||
                path.equals("/favicon.ico") ||
                path.equals("/") ||
                path.startsWith("/error");

        // Allow OPTIONS requests (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        log.debug("Path: {}, Method: {}, isPublic: {}", path, method, isPublicEndpoint);
        return isPublicEndpoint;
    }

    /**
     * Extract username from token
     */
    private String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract authorities from token
     */
    @SuppressWarnings("unchecked")
    private List<String> extractAuthorities(String token) {
        return extractClaim(token, claims -> (List<String>) claims.get("authorities"));
    }

    /**
     * Extract claim from token
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract all claims from token
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
     * Check if token is valid
     */
    private boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            boolean isNotExpired = claims.getExpiration().after(new java.util.Date());
            boolean isAccessToken = "ACCESS".equals(claims.get("type"));

            log.debug("Token expiration: {}, Is not expired: {}, Is access token: {}",
                    claims.getExpiration(), isNotExpired, isAccessToken);

            return isNotExpired && isAccessToken;
        } catch (JwtException e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get signing key
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
}