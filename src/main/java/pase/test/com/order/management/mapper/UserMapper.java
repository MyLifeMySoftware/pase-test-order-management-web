package pase.test.com.order.management.mapper;

import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import pase.test.com.database.entity.user.Role;
import pase.test.com.database.entity.user.User;
import pase.test.com.database.entity.user.UserPermission;
import pase.test.com.order.management.dto.PermissionResponse;
import pase.test.com.order.management.dto.RoleResponse;
import pase.test.com.order.management.dto.UserListResponse;
import pase.test.com.order.management.dto.UserResponse;

@Component
public class UserMapper {

    /**
     * Convert User entity to UserResponse DTO
     */
    public UserResponse toUserResponse(User user) {
        if (user == null) {
            return null;
        }

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .enabled(user.isEnabled())
                .accountNonExpired(user.isAccountNonExpired())
                .accountNonLocked(user.isAccountNonLocked())
                .credentialsNonExpired(user.isCredentialsNonExpired())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLogin(user.getLastLogin())
                .roles(mapRoles(user.getRoles()))
                .build();
    }

    /**
     * Convert User entity to UserListResponse DTO
     */
    public UserListResponse toUserListResponse(User user) {
        if (user == null) {
            return null;
        }

        Set<String> roleNames = user.getRoles() != null
                ? user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet())
                : Set.of();

        return UserListResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .roleNames(roleNames)
                .build();
    }

    /**
     * Convert Role entity to RoleResponse DTO
     */
    public RoleResponse toRoleResponse(Role role) {
        if (role == null) {
            return null;
        }

        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .active(role.getActive())
                .createdAt(role.getCreatedAt())
                .permissions(mapPermissions(role.getPermissions()))
                .build();
    }

    /**
     * Convert UserPermission entity to PermissionResponse DTO
     */
    public PermissionResponse toPermissionResponse(UserPermission permission) {
        if (permission == null) {
            return null;
        }

        return PermissionResponse.builder()
                .id(permission.getId())
                .name(permission.getName())
                .description(permission.getDescription())
                .resource(permission.getResource())
                .action(permission.getAction())
                .active(permission.getActive())
                .build();
    }

    /**
     * Map roles from entities to DTOs
     */
    private Set<RoleResponse> mapRoles(Set<Role> roles) {
        if (roles == null) {
            return Set.of();
        }

        return roles.stream()
                .map(this::toRoleResponse)
                .collect(Collectors.toSet());
    }

    /**
     * Map permissions from entities to DTOs
     */
    private Set<PermissionResponse> mapPermissions(Set<UserPermission> permissions) {
        if (permissions == null) {
            return Set.of();
        }

        return permissions.stream()
                .map(this::toPermissionResponse)
                .collect(Collectors.toSet());
    }
}