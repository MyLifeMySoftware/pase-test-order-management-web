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
            log.debug("No Authorization header or doesn't start with Bearer");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Extract JWT token
            jwt = authHeader.substring(7);

            // Validate token structure first
            if (!jwtService.validateTokenStructure(jwt)) {
                log.debug("Invalid JWT token structure");
                filterChain.doFilter(request, response);
                return;
            }

            // Check if it's an access token and is valid
            if (!jwtService.isAccessToken(jwt) || !jwtService.isTokenValid(jwt)) {
                log.debug("Token is not an access token or is invalid");
                filterChain.doFilter(request, response);
                return;
            }

            // Extract username from token
            username = jwtService.extractUsername(jwt);

            // Check if user is not already authenticated
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Extract authorities from token
                List<String> tokenAuthorities = jwtService.extractAuthorities(jwt);
                List<SimpleGrantedAuthority> authorities = tokenAuthorities.stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList();

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
        } catch (Exception e) {
            log.error("Error processing JWT token: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // Skip JWT validation for public endpoints
        boolean isPublicEndpoint =
                path.startsWith("/api/v1/management/health") ||
                        path.startsWith("/actuator") ||
                        path.startsWith("/swagger-ui") ||
                        path.startsWith("/v3/api-docs") ||
                        path.startsWith("/swagger-resources") ||
                        path.startsWith("/webjars") ||
                        path.equals("/favicon.ico") ||
                        path.equals("/") ||
                        path.startsWith("/error");

        // Allow OPTIONS requests (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        if (isPublicEndpoint) {
            log.debug("Skipping JWT filter for public endpoint: {}", path);
        } else {
            log.debug("Processing JWT filter for protected endpoint: {}", path);
        }

        return isPublicEndpoint;
    }
}
