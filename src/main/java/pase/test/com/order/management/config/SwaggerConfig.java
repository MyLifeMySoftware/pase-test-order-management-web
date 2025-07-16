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
                            🔐 **User and System Management Service for Pase Project**
                            
                            **How to test JWT authentication:**
                            
                            **Step 1: Get JWT Token from Auth Service**
                            1. Open Auth Service: http://localhost:8080/swagger-ui.html
                            2. Use `/api/v1/auth/login` endpoint
                            3. Login with test credentials:
                               - **admin** / **Admin123!** (ADMIN role)
                               - **testuser** / **Test123!** (USER role)
                               - **moderator** / **Mod123!** (MODERATOR role)
                            4. Copy the `accessToken` from the response
                            
                            **Step 2: Use Token in Management Service**
                            1. Click the "Authorize" button below 🔒
                            2. Enter: `YOUR_ACCESS_TOKEN_HERE`
                            3. Click "Authorize"
                            4. Now you can test protected endpoints!
                           
                            **Token expires in 15 minutes** - get a new one if needed!
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
                                .description("🔧 Management Service - Ready for JWT!"),
                        new Server()
                                .url("http://localhost:8080")
                                .description("🔑 Auth Service - Get tokens here first!")))
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
                    🔐 **JWT Bearer Token Authentication**
                    
                    **Format:** Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
                    
                    **To get a valid token:**
                    1. Go to: http://localhost:8080/swagger-ui.html
                    2. POST to `/api/v1/auth/login`
                    3. Use credentials: admin/Admin123! or testuser/Test123!
                    4. Copy the `accessToken` field
                    5. Paste here with "Bearer " prefix
                    
                    **Example:**
                    ```
                    eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI6MTYxNjIzOTAyMn0.abc123...
                    ```
                    """);
    }
}