package pase.test.com.order.management.config;

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
        log.info("Configuring security filter chain for Order Management service...");

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .authorizeHttpRequests(authz -> {
                    log.info("Configuring authorization rules for Order Management...");
                    authz
                            // Health checks and docs
                            .requestMatchers(
                                    "/actuator/**",
                                    "/swagger-ui/**",
                                    "/swagger-ui.html",
                                    "/v3/api-docs/**",
                                    "/swagger-resources/**",
                                    "/webjars/**",
                                    "/favicon.ico",
                                    "/error"
                            ).permitAll()

                            // OPTIONS for CORS
                            .requestMatchers("OPTIONS").permitAll()

                            // Admin-only endpoints for order management
                            .requestMatchers(
                                    "/api/v1/orders/admin/**",
                                    "/api/v1/orders/statistics",
                                    "/api/v1/orders/reports/**",
                                    "/api/v1/orders/bulk/**"
                            ).hasRole("ADMIN")

                            // Moderator and Admin endpoints
                            .requestMatchers(
                                    "/api/v1/orders/all",
                                    "/api/v1/orders/search",
                                    "/api/v1/orders/*/status"
                            ).hasAnyRole("ADMIN", "MODERATOR")

                            // User endpoints (all authenticated users can manage their own orders)
                            .requestMatchers(
                                    "/api/v1/orders/my-orders",
                                    "/api/v1/orders/create",
                                    "/api/v1/orders/*/cancel"
                            ).hasAnyRole("USER", "ADMIN", "MODERATOR")

                            // Specific order access (users can only access their own orders)
                            .requestMatchers(
                                    "/api/v1/orders/{id}"
                            ).hasAnyRole("USER", "ADMIN", "MODERATOR")

                            // Health endpoint
                            .requestMatchers("/api/v1/orders/health").permitAll()

                            // All other requests require authentication
                            .anyRequest().authenticated();
                })
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        log.info("Security filter chain configured successfully for Order Management service");
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(java.util.List.of("*"));
        configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(java.util.List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}