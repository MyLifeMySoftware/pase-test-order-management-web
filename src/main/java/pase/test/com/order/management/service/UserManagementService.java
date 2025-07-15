package pase.test.com.order.management.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pase.test.com.database.entity.user.Role;
import pase.test.com.database.entity.user.User;
import pase.test.com.database.exception.auth.UserAlreadyExistsException;
import pase.test.com.database.exception.auth.UserNotFoundException;
import pase.test.com.database.repository.user.RoleRepository;
import pase.test.com.database.repository.user.UserRepository;
import pase.test.com.order.management.dto.UserCreateRequest;
import pase.test.com.order.management.dto.UserListResponse;
import pase.test.com.order.management.dto.UserResponse;
import pase.test.com.order.management.dto.UserUpdateRequest;
import pase.test.com.order.management.mapper.UserMapper;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserManagementService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    /**
     * Get user profile by username
     */
    public UserResponse getUserProfile(String username) {
        log.debug("Getting profile for user: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));

        return userMapper.toUserResponse(user);
    }

    /**
     * Update user profile
     */
    @Transactional
    public UserResponse updateUserProfile(String username, UserUpdateRequest request) {
        log.debug("Updating profile for user: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));

        // Update allowed fields for profile update
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getEmail() != null) {
            // Check if email is already taken by another user
            userRepository.findByEmail(request.getEmail())
                    .filter(existingUser -> !existingUser.getId().equals(user.getId()))
                    .ifPresent(existingUser -> {
                        throw new UserAlreadyExistsException("Email already exists: " + request.getEmail());
                    });
            user.setEmail(request.getEmail());
        }

        User savedUser = userRepository.save(user);
        log.info("Profile updated successfully for user: {}", username);

        return userMapper.toUserResponse(savedUser);
    }

    /**
     * Get all users with pagination
     */
    public Page<UserListResponse> getAllUsers(Pageable pageable) {
        log.debug("Getting all users with pagination: {}", pageable);

        Page<User> users = userRepository.findAll(pageable);
        return users.map(userMapper::toUserListResponse);
    }

    /**
     * Get user by ID
     */
    public UserResponse getUserById(Long userId) {
        log.debug("Getting user by ID: {}", userId);

        User user = userRepository.findById(userId.toString())
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        return userMapper.toUserResponse(user);
    }

    /**
     * Search users by username or email
     */
    public List<UserListResponse> searchUsers(String query) {
        log.debug("Searching users with query: {}", query);

        List<User> users = userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                query, query);

        return users.stream()
                .map(userMapper::toUserListResponse)
                .collect(Collectors.toList());
    }

    /**
     * Create new user
     */
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        log.debug("Creating user: {}", request.getUsername());

        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already exists: " + request.getUsername());
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists: " + request.getEmail());
        }

        // Get roles
        Set<Role> roles = request.getRoleIds().stream()
                .map(roleId -> roleRepository.findById(roleId)
                        .orElseThrow(() -> new UserNotFoundException("Role not found with ID: " + roleId)))
                .collect(Collectors.toSet());

        // Create user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .enabled(request.isEnabled())
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .roles(roles)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User created successfully: {}", request.getUsername());

        return userMapper.toUserResponse(savedUser);
    }

    /**
     * Update user by ID
     */
    @Transactional
    public UserResponse updateUser(Long userId, UserUpdateRequest request) {
        log.debug("Updating user with ID: {}", userId);

        User user = userRepository.findById(userId.toString())
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        // Update fields
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getEmail() != null) {
            // Check if email is already taken by another user
            userRepository.findByEmail(request.getEmail())
                    .filter(existingUser -> !existingUser.getId().equals(user.getId()))
                    .ifPresent(existingUser -> {
                        throw new UserAlreadyExistsException("Email already exists: " + request.getEmail());
                    });
            user.setEmail(request.getEmail());
        }
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        User savedUser = userRepository.save(user);
        log.info("User updated successfully with ID: {}", userId);

        return userMapper.toUserResponse(savedUser);
    }

    /**
     * Delete user by ID
     */
    @Transactional
    public void deleteUser(Long userId) {
        log.debug("Deleting user with ID: {}", userId);

        User user = userRepository.findById(userId.toString())
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        userRepository.delete(user);
        log.info("User deleted successfully with ID: {}", userId);
    }

    /**
     * Update user status (enable/disable)
     */
    @Transactional
    public UserResponse updateUserStatus(Long userId, boolean enabled) {
        log.debug("Updating user status for ID: {} to {}", userId, enabled);

        User user = userRepository.findById(userId.toString())
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        user.setEnabled(enabled);
        User savedUser = userRepository.save(user);

        log.info("User status updated successfully for ID: {} to {}", userId, enabled);
        return userMapper.toUserResponse(savedUser);
    }

    /**
     * Assign roles to user
     */
    @Transactional
    public UserResponse assignRolesToUser(Long userId, List<Long> roleIds) {
        log.debug("Assigning roles {} to user with ID: {}", roleIds, userId);

        User user = userRepository.findById(userId.toString())
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        Set<Role> roles = roleIds.stream()
                .map(roleId -> roleRepository.findById(roleId)
                        .orElseThrow(() -> new UserNotFoundException("Role not found with ID: " + roleId)))
                .collect(Collectors.toSet());

        user.setRoles(roles);
        User savedUser = userRepository.save(user);

        log.info("Roles assigned successfully to user with ID: {}", userId);
        return userMapper.toUserResponse(savedUser);
    }

    /**
     * Get user statistics
     */
    public Object getUserStatistics() {
        log.debug("Getting user statistics");

        long totalUsers = userRepository.count();
        long enabledUsers = userRepository.countByEnabled(true);
        long disabledUsers = userRepository.countByEnabled(false);

        // Get users created in the last 30 days
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long recentUsers = userRepository.countByCreatedAtAfter(thirtyDaysAgo);

        // Get role distribution
        Map<String, Long> roleDistribution = new HashMap<>();
        roleRepository.findAll().forEach(role -> {
            long userCount = userRepository.countByRolesContaining(role);
            roleDistribution.put(role.getName(), userCount);
        });

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalUsers", totalUsers);
        statistics.put("enabledUsers", enabledUsers);
        statistics.put("disabledUsers", disabledUsers);
        statistics.put("recentUsers", recentUsers);
        statistics.put("roleDistribution", roleDistribution);
        statistics.put("timestamp", LocalDateTime.now());

        log.info("User statistics retrieved successfully");
        return statistics;
    }
}