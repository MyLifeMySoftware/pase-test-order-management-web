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

        // Skip for public endpoints
        if (shouldNotFilter(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract Authorization header
        final String authHeader = request.getHeader("Authorization");

        if (!StringUtils.hasText(authHeader)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Extract JWT token
            final String jwt = authHeader.substring(7).trim();

            if (jwt.isEmpty()) {
                filterChain.doFilter(request, response);
                return;
            }

            // Validate token structure
            if (!jwtService.validateTokenStructure(jwt)) {
                filterChain.doFilter(request, response);
                return;
            }

            // Check if it's an access token
            if (!jwtService.isAccessToken(jwt)) {
                filterChain.doFilter(request, response);
                return;
            }

            // Validate token (expiration, issuer, etc.)
            if (!jwtService.isTokenValid(jwt)) {
                filterChain.doFilter(request, response);
                return;
            }

            // Extract username
            final String username = jwtService.extractUsername(jwt);

            if (!StringUtils.hasText(username)) {
                filterChain.doFilter(request, response);
                return;
            }

            // Check if already authenticated (avoid re-processing)
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                filterChain.doFilter(request, response);
                return;
            }

            // Extract authorities
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

            // Set authentication details
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Set authentication in security context
            SecurityContextHolder.getContext().setAuthentication(authToken);

        } catch (JwtException e) {

            SecurityContextHolder.clearContext();

        } catch (Exception e) {
            log.error("Unexpected error processing JWT for {}: {}", path, e.getMessage(), e);
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

        // Public endpoints that don't need JWT validation
        boolean isPublic = path.equals("/") ||
                path.equals("/favicon.ico") ||
                path.startsWith("/error") ||
                path.startsWith("/swagger-ui") ||
                path.equals("/swagger-ui.html") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/swagger-resources") ||
                path.startsWith("/webjars") ||
                path.startsWith("/actuator") ||
                path.equals("/api/v1/management/health") ||
                path.equals("/api/v1/orders/health") ||
                path.startsWith("/api/v1/test/public");

        if (isPublic) {
            log.debug("Public endpoint detected: {}", path);
        } else {
            log.debug("Protected endpoint detected: {}", path);
        }

        return isPublic;
    }
}