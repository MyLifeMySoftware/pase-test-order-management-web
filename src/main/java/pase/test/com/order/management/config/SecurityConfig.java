package pase.test.com.order.management.config;

import java.util.Arrays;
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
        log.info("Configuring JWT-Only Security (No Default Login)");

        return http
                // ===== DISABLE ALL DEFAULT AUTHENTICATION =====
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)          // No HTTP Basic
                .formLogin(AbstractHttpConfigurer::disable)          // No Form Login
                .logout(AbstractHttpConfigurer::disable)             // No Default Logout
                .anonymous(conf -> conf.disable())                   // No Anonymous
                .rememberMe(AbstractHttpConfigurer::disable)         // No Remember Me

                // ===== CORS =====
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ===== STATELESS SESSION =====
                .sessionManagement(session -> {
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
                    session.maximumSessions(1).maxSessionsPreventsLogin(false);
                })

                // ===== EXCEPTION HANDLING =====
                .exceptionHandling(ex -> {
                    ex.authenticationEntryPoint(jwtAuthenticationEntryPoint);
                    ex.accessDeniedHandler((request, response, accessDeniedException) -> {
                        log.error("Access denied for: {} - {}", request.getRequestURI(), accessDeniedException.getMessage());
                        response.setStatus(403);
                        response.setContentType("application/json");
                        response.getWriter().write(
                                "{\"error\":\"Access Denied\",\"message\":\"Insufficient permissions\",\"path\":\"" +
                                        request.getRequestURI() + "\"}"
                        );
                    });
                })

                // ===== AUTHORIZATION RULES =====
                .authorizeHttpRequests(authz -> {
                    log.info("Configuring authorization rules for JWT-only access");
                    authz
                            // ===== COMPLETELY PUBLIC =====
                            .requestMatchers(
                                    "/",
                                    "/favicon.ico",
                                    "/error",
                                    "/error/**",
                                    // Swagger UI
                                    "/swagger-ui/**",
                                    "/swagger-ui.html",
                                    "/v3/api-docs/**",
                                    "/swagger-resources/**",
                                    "/webjars/**",
                                    // Health checks
                                    "/api/v1/management/health",
                                    "/api/v1/orders/health",
                                    "/api/v1/test/public/**",
                                    // Actuator
                                    "/actuator/**"
                            ).permitAll()

                            // ===== JWT REQUIRED FOR EVERYTHING ELSE =====
                            .anyRequest().authenticated();
                })

                // ===== ADD JWT FILTER =====
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        log.info("Configuring CORS for JWT requests");
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}