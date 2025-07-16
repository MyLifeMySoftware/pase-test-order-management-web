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
                            User and Order Management Service for Pase Project
                            
                            **Features:**
                            - User Management (CRUD operations)
                            - Driver Management (Registration and status control)
                            - Order Management (Complete lifecycle)
                            - File Attachments (PDF and Images)
                            - Role-based Access Control
                            
                            **How to use:**
                            1. Get JWT token from Auth Service (http://localhost:8080/swagger-ui.html)
                            2. Login with: admin/Admin123!, testuser/Test123!, or moderator/Mod123!
                            3. Copy the accessToken and click 'Authorize' below
                            4. Enter: Bearer YOUR_ACCESS_TOKEN
                            5. Test the endpoints
                            
                            **Order Management Workflow:**
                            1. Create Order (status: CREATED)
                            2. Assign Driver (status: ASSIGNED)
                            3. Add Attachments (PDF/Images)
                            4. Update Status (IN_PROGRESS → COMPLETED)
                            
                            **Available Roles:**
                            - USER: View orders, drivers
                            - MODERATOR: Create/assign orders, manage drivers
                            - ADMIN: Full access to all operations
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
                        .addSecuritySchemes("Bearer Authentication", createJwtScheme()));
    }

    private SecurityScheme createJwtScheme() {
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