package pase.test.com.order.management.security.jwt;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Skip JWT validation for public endpoints
        if (isPublicEndpoint(path) || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            log.debug("Public endpoint: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");

        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            log.debug("No valid Authorization header for: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Extract and clean JWT token
            final String jwt = authHeader.substring(7).trim(); // Remove "Bearer " and trim whitespace

            log.debug("Processing JWT for path: {}, token length: {}", path, jwt.length());

            if (!jwtService.validateTokenStructure(jwt)) {
                log.debug("❌ Invalid JWT token structure");
                filterChain.doFilter(request, response);
                return;
            }

            if (!jwtService.isAccessToken(jwt)) {
                log.debug("❌ Token is not an access token");
                filterChain.doFilter(request, response);
                return;
            }

            if (!jwtService.isTokenValid(jwt)) {
                log.debug("❌ Token is not valid (expired or malformed)");
                filterChain.doFilter(request, response);
                return;
            }

            final String username = jwtService.extractUsername(jwt);

            // CRITICAL: Check if already authenticated to avoid re-processing
            if (StringUtils.hasText(username) && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Extract authorities from JWT
                List<String> tokenAuthorities = jwtService.extractAuthorities(jwt);
                if (tokenAuthorities == null) {
                    tokenAuthorities = List.of();
                }

                List<SimpleGrantedAuthority> authorities = tokenAuthorities.stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList();

                // Create authentication token
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        username, null, authorities);

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // CRITICAL: Set authentication in context
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("User {} authenticated with authorities: {}", username, authorities);
                log.debug("SecurityContext set for request: {}", path);
            } else if (SecurityContextHolder.getContext().getAuthentication() != null) {
                log.debug("User already authenticated: {}",
                        SecurityContextHolder.getContext().getAuthentication().getName());
            }

        } catch (JwtException e) {
            log.error("JWT validation failed: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        } catch (Exception e) {
            log.error("Error processing JWT: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(String path) {
        boolean isPublic = path.equals("/") ||
                path.equals("/favicon.ico") ||
                path.startsWith("/error") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/swagger-resources") ||
                path.startsWith("/webjars") ||
                path.startsWith("/actuator") ||
                path.equals("/api/v1/management/health") ||
                path.equals("/api/v1/orders/health") ||
                path.startsWith("/api/v1/test/");

        if (isPublic) {
            log.debug("Public endpoint detected: {}", path);
        } else {
            log.debug("Protected endpoint detected: {}", path);
        }

        return isPublic;
    }
}