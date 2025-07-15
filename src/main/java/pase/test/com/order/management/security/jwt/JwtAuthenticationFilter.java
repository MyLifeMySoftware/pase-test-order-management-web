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
        log.debug("Processing request: {} {}", request.getMethod(), path);

        // Check if this request should skip JWT validation
        if (shouldNotFilter(request)) {
            log.debug("Skipping JWT authentication for: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");

        // If no Authorization header, continue without authentication
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            log.debug("No valid Authorization header found for: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Extract JWT token
            final String jwt = authHeader.substring(7);
            log.debug("Extracted JWT token (length: {})", jwt.length());

            // Validate token structure first
            if (!jwtService.validateTokenStructure(jwt)) {
                log.debug("Invalid JWT token structure");
                filterChain.doFilter(request, response);
                return;
            }

            // Check if it's an access token and is valid
            if (!jwtService.isAccessToken(jwt)) {
                log.debug("Token is not an access token");
                filterChain.doFilter(request, response);
                return;
            }

            if (!jwtService.isTokenValid(jwt)) {
                log.debug("Token is not valid (expired or malformed)");
                filterChain.doFilter(request, response);
                return;
            }

            // Extract username from token
            final String username = jwtService.extractUsername(jwt);
            log.debug("Extracted username from JWT: {}", username);

            // Check if user is not already authenticated
            if (StringUtils.hasText(username) && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Extract authorities from token
                List<String> tokenAuthorities = jwtService.extractAuthorities(jwt);
                if (tokenAuthorities == null) {
                    tokenAuthorities = List.of(); // Fallback to empty list
                }

                List<SimpleGrantedAuthority> authorities = tokenAuthorities.stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList();

                log.debug("Extracted authorities: {}", authorities);

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

                log.debug("User {} authenticated successfully via JWT with authorities: {}",
                        username, authorities);
            }
        } catch (JwtException e) {
            log.error("JWT token validation failed: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        } catch (Exception e) {
            log.error("Error processing JWT token: {}", e.getMessage(), e);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // Always allow OPTIONS requests (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        // Skip JWT validation for these public endpoints
        boolean isPublicEndpoint =
                // Health checks
                path.equals("/api/v1/management/health") ||
                        path.equals("/api/v1/orders/health") ||

                        // Swagger UI and API docs
                        path.startsWith("/swagger-ui") ||
                        path.equals("/swagger-ui.html") ||
                        path.startsWith("/v3/api-docs") ||
                        path.startsWith("/swagger-resources") ||
                        path.startsWith("/webjars") ||

                        // Actuator
                        path.startsWith("/actuator") ||

                        // Static resources
                        path.equals("/favicon.ico") ||
                        path.equals("/") ||
                        path.equals("/index.html") ||
                        path.startsWith("/error");

        if (isPublicEndpoint) {
            log.debug("Public endpoint, skipping JWT validation: {}", path);
        } else {
            log.debug("Protected endpoint, JWT validation required: {}", path);
        }

        return isPublicEndpoint;
    }
}