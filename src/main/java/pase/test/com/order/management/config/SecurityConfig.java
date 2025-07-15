package pase.test.com.order.management.config;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import pase.test.com.order.management.security.jwt.JwtAuthenticationEntryPoint;
import pase.test.com.order.management.security.jwt.JwtAuthenticationFilter;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("🔧 Configuring JWT-ONLY Security for Management Service...");

        return http
                // ===== DISABLE ALL DEFAULT AUTHENTICATION MECHANISMS =====
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)  // 🚫 NO FORM LOGIN
                .logout(AbstractHttpConfigurer::disable)
                .rememberMe(AbstractHttpConfigurer::disable)
                .anonymous(AbstractHttpConfigurer::disable)  // 🚫 NO ANONYMOUS

                // ===== CORS CONFIGURATION =====
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ===== STATELESS SESSION MANAGEMENT =====
                .sessionManagement(session -> {
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
                    session.sessionFixation().none();  // No session fixation protection needed
                    session.maximumSessions(0);  // No concurrent sessions
                })

                // ===== EXCEPTION HANDLING - NO REDIRECTS =====
                .exceptionHandling(exception -> {
                    exception.authenticationEntryPoint(jwtAuthenticationEntryPoint);
                    exception.accessDeniedHandler((request, response, accessDeniedException) -> {
                        log.debug("Access denied for: {} - {}", request.getRequestURI(), accessDeniedException.getMessage());
                        response.setStatus(403);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"Access Denied\",\"message\":\"Insufficient permissions\"}");
                    });
                })

                // ===== URL AUTHORIZATION - VERY EXPLICIT =====
                .authorizeHttpRequests(authz -> {
                    log.info("🔐 Configuring URL authorization rules...");

                    authz
                            // 🟢 COMPLETELY PUBLIC - NO AUTH REQUIRED
                            .requestMatchers(
                                    new AntPathRequestMatcher("/swagger-ui/**"),
                                    new AntPathRequestMatcher("/swagger-ui.html"),
                                    new AntPathRequestMatcher("/v3/api-docs/**"),
                                    new AntPathRequestMatcher("/swagger-resources/**"),
                                    new AntPathRequestMatcher("/webjars/**"),
                                    new AntPathRequestMatcher("/api/v1/management/health"),
                                    new AntPathRequestMatcher("/api/v1/orders/health"),
                                    new AntPathRequestMatcher("/actuator/**"),
                                    new AntPathRequestMatcher("/favicon.ico"),
                                    new AntPathRequestMatcher("/error"),
                                    new AntPathRequestMatcher("/")
                            ).permitAll()

                            // 🔒 PROTECTED ENDPOINTS - JWT REQUIRED
                            .requestMatchers(
                                    new AntPathRequestMatcher("/api/v1/management/**"),
                                    new AntPathRequestMatcher("/api/v1/orders/**")
                            ).authenticated()

                            // 🟡 FALLBACK - DENY BY DEFAULT (safer)
                            .anyRequest().denyAll();
                })

                // ===== ADD JWT FILTER =====
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        log.info("🌐 Configuring CORS...");
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}