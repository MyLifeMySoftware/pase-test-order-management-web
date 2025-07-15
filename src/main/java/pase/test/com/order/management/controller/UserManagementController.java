package pase.test.com.order.management.controller;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pase.test.com.database.dto.ApiResponse;
import pase.test.com.order.management.dto.UserCreateRequest;
import pase.test.com.order.management.dto.UserListResponse;
import pase.test.com.order.management.dto.UserResponse;
import pase.test.com.order.management.dto.UserUpdateRequest;
import pase.test.com.order.management.service.UserManagementService;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User management operations with role-based access control")
@SecurityRequirement(name = "Bearer Authentication")
public class UserManagementController {

    private final UserManagementService userManagementService;

    /**
     * Get current user profile - Available to all authenticated users
     */
    @GetMapping("/profile")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('MODERATOR')")
    @Timed(value = "user.profile.get", description = "Time taken to get user profile")
    @Operation(summary = "Get current user profile", description = "Get the profile of the currently authenticated user")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUserProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();

        log.info("Getting profile for user: {}", currentUsername);

        UserResponse userResponse = userManagementService.getUserProfile(currentUsername);
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", userResponse));
    }

    /**
     * Update current user profile - Available to all authenticated users
     */
    @PutMapping("/update-profile")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('MODERATOR')")
    @Timed(value = "user.profile.update", description = "Time taken to update user profile")
    @Operation(summary = "Update current user profile", description = "Update the profile of the currently authenticated user")
    public ResponseEntity<ApiResponse<UserResponse>> updateCurrentUserProfile(
            @Valid @RequestBody UserUpdateRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();

        log.info("Updating profile for user: {}", currentUsername);

        UserResponse userResponse = userManagementService.updateUserProfile(currentUsername, request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", userResponse));
    }

    /**
     * Get all users - Available to ADMIN and MODERATOR only
     */
    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    @Timed(value = "user.list.get", description = "Time taken to get user list")
    @Operation(summary = "Get all users", description = "Get paginated list of all users - Admin and Moderator only")
    public ResponseEntity<ApiResponse<Page<UserListResponse>>> getAllUsers(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "username") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "ASC") String sortDir) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("Getting user list requested by: {} with authorities: {}",
                auth.getName(), auth.getAuthorities());

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.fromString(sortDir), sortBy));

        Page<UserListResponse> users = userManagementService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
    }

    /**
     * Get user by ID - Available to ADMIN and MODERATOR only
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    @Timed(value = "user.get", description = "Time taken to get user by ID")
    @Operation(summary = "Get user by ID", description = "Get user details by ID - Admin and Moderator only")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @Parameter(description = "User ID") @PathVariable Long userId) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("Getting user {} requested by: {}", userId, auth.getName());

        UserResponse userResponse = userManagementService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", userResponse));
    }

    /**
     * Search users - Available to ADMIN and MODERATOR only
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    @Timed(value = "user.search", description = "Time taken to search users")
    @Operation(summary = "Search users", description = "Search users by username or email - Admin and Moderator only")
    public ResponseEntity<ApiResponse<List<UserListResponse>>> searchUsers(
            @Parameter(description = "Search query") @RequestParam String query) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("Searching users with query '{}' requested by: {}", query, auth.getName());

        List<UserListResponse> users = userManagementService.searchUsers(query);
        return ResponseEntity.ok(ApiResponse.success("Users found successfully", users));
    }

    /**
     * Create new user - ADMIN only
     */
    @PostMapping("/admin/create")
    @PreAuthorize("hasRole('ADMIN')")
    @Timed(value = "user.create", description = "Time taken to create user")
    @Operation(summary = "Create new user", description = "Create a new user - Admin only")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody UserCreateRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("Creating user '{}' requested by admin: {}", request.getUsername(), auth.getName());

        UserResponse userResponse = userManagementService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User created successfully", userResponse));
    }

    /**
     * Update user by ID - ADMIN only
     */
    @PutMapping("/admin/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Timed(value = "user.update", description = "Time taken to update user")
    @Operation(summary = "Update user", description = "Update user by ID - Admin only")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Valid @RequestBody UserUpdateRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("Updating user {} requested by admin: {}", userId, auth.getName());

        UserResponse userResponse = userManagementService.updateUser(userId, request);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", userResponse));
    }

    /**
     * Delete user by ID - ADMIN only
     */
    @DeleteMapping("/admin/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Timed(value = "user.delete", description = "Time taken to delete user")
    @Operation(summary = "Delete user", description = "Delete user by ID - Admin only")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @Parameter(description = "User ID") @PathVariable Long userId) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("Deleting user {} requested by admin: {}", userId, auth.getName());

        userManagementService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
    }
}