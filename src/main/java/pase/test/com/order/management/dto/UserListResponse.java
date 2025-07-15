package pase.test.com.order.management.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserListResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    private Set<String> roleNames;
}
