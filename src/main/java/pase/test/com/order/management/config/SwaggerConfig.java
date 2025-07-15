package pase.test.com.order.management.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Pase Management Service API")
                        .description("""
                            User and System Management Service for Pase Project
                            
                            **How to use:**
                            1. First, get a JWT token from the Auth Service (http://localhost:8080/swagger-ui.html)
                            2. Login with: username='admin', password='Admin123!'
                            3. Copy the accessToken from the response
                            4. Click the 'Authorize' button below and enter: Bearer YOUR_ACCESS_TOKEN
                            5. Now you can test all protected endpoints
                            
                            **Available test users:**
                            - admin / Admin123! (ADMIN role)
                            - testuser / Test123! (USER role)
                            - moderator / Mod123! (MODERATOR role)
                            """)
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Erick Antonio Reyes Montalvo")
                                .email("montalvoerickantonio@gmail.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8081")
                                .description("Management Development server"),
                        new Server()
                                .url("http://localhost:8080")
                                .description("Auth Service (for getting tokens)")))
                .addSecurityItem(new SecurityRequirement()
                        .addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication", createJWTScheme()));
    }

    private SecurityScheme createJWTScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .bearerFormat("JWT")
                .scheme("bearer")
                .description("""
                    Enter JWT Bearer token obtained from Auth Service
                    
                    Format: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
                    
                    To get a token:
                    1. Go to Auth Service: http://localhost:8080/swagger-ui.html
                    2. Use /api/v1/auth/login endpoint
                    3. Login with admin/Admin123! or testuser/Test123!
                    4. Copy the 'accessToken' from response
                    5. Use it here with 'Bearer ' prefix
                    """);
    }
}