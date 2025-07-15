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
public class RoleResponse {
    private Long id;
    private String name;
    private String description;
    private boolean active;
    private LocalDateTime createdAt;
    private Set<PermissionResponse> permissions;
}
