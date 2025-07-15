package pase.test.com.order.management.controller;

import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pase.test.com.database.dto.ApiResponse;
import pase.test.com.order.management.dto.UserCreateRequest;
import pase.test.com.order.management.dto.UserResponse;
import pase.test.com.order.management.dto.UserUpdateRequest;
import pase.test.com.order.management.service.UserManagementService;

@Slf4j
@RestController
@RequestMapping("/api/v1/management/users")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserManagementService userManagementService;

    /**
     * Get all users with pagination
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('user:read')")
    @Timed(value = "management.users.list", description = "Time taken to list users")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("Getting all users with pagination: {}", pageable);
        Page<UserResponse> users = userManagementService.getAllUsers(pageable);

        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
    }

    /**
     * Get user by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('user:read')")
    @Timed(value = "management.users.get", description = "Time taken to get user by ID")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {

        log.info("Getting user by ID: {}", id);
        UserResponse user = userManagementService.getUserById(id);

        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", user));
    }

    /**
     * Get user by username
     */
    @GetMapping("/username/{username}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('user:read')")
    @Timed(value = "management.users.get.username", description = "Time taken to get user by username")
    public ResponseEntity<ApiResponse<UserResponse>> getUserByUsername(@PathVariable String username) {

        log.info("Getting user by username: {}", username);
        UserResponse user = userManagementService.getUserByUsername(username);

        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", user));
    }

    /**
     * Create new user
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('user:write')")
    @Timed(value = "management.users.create", description = "Time taken to create user")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody UserCreateRequest request) {

        log.info("Creating new user: {}", request.getUsername());
        UserResponse user = userManagementService.createUser(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User created successfully", user));
    }

    /**
     * Update user
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('user:write')")
    @Timed(value = "management.users.update", description = "Time taken to update user")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {

        log.info("Updating user with ID: {}", id);
        UserResponse user = userManagementService.updateUser(id, request);

        return ResponseEntity.ok(ApiResponse.success("User updated successfully", user));
    }

    /**
     * Delete user
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('user:delete')")
    @Timed(value = "management.users.delete", description = "Time taken to delete user")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {

        log.info("Deleting user with ID: {}", id);
        userManagementService.deleteUser(id);

        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
    }

    /**
     * Enable/Disable user
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('user:write')")
    @Timed(value = "management.users.toggle.status", description = "Time taken to toggle user status")
    public ResponseEntity<ApiResponse<UserResponse>> toggleUserStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request) {

        boolean enabled = request.getOrDefault("enabled", true);
        log.info("Toggling user status for ID: {} to enabled: {}", id, enabled);

        UserResponse user = userManagementService.toggleUserStatus(id, enabled);

        return ResponseEntity.ok(ApiResponse.success("User status updated successfully", user));
    }

    /**
     * Lock/Unlock user account
     */
    @PatchMapping("/{id}/lock")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('user:write')")
    @Timed(value = "management.users.toggle.lock", description = "Time taken to toggle user lock")
    public ResponseEntity<ApiResponse<UserResponse>> toggleUserLock(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request) {

        boolean locked = request.getOrDefault("locked", false);
        log.info("Toggling user lock status for ID: {} to locked: {}", id, locked);

        UserResponse user = userManagementService.toggleUserLock(id, locked);

        return ResponseEntity.ok(ApiResponse.success("User lock status updated successfully", user));
    }

    /**
     * Search users
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR') or hasAuthority('user:read')")
    @Timed(value = "management.users.search", description = "Time taken to search users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> searchUsers(
            @RequestParam String q) {

        log.info("Searching users with term: {}", q);
        List<UserResponse> users = userManagementService.searchUsers(q);

        return ResponseEntity.ok(ApiResponse.success("Users search completed", users));
    }
}