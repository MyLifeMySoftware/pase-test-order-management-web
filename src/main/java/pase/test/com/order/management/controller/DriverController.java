package pase.test.com.order.management.controller;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pase.test.com.database.dto.ApiResponse;
import pase.test.com.database.dto.driver.DriverCreateRequest;
import pase.test.com.database.dto.driver.DriverResponse;
import pase.test.com.order.management.service.driver.DriverService;

@Slf4j
@RestController
@RequestMapping("/api/v1/drivers")
@RequiredArgsConstructor
@Tag(name = "Driver Management", description = "Driver registration and management operations")
@SecurityRequirement(name = "Bearer Authentication")
public class DriverController {

    private final DriverService driverService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    @Timed(value = "driver.create", description = "Time taken to create driver")
    @Operation(summary = "Create new driver", description = "Register a new driver in the system")
    public ResponseEntity<ApiResponse<DriverResponse>> createDriver(
            @Valid @RequestBody DriverCreateRequest request) {

        log.info("Creating new driver: {}", request.getDriverName());
        DriverResponse driver = driverService.createDriver(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Driver created successfully", driver));
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('MODERATOR')")
    @Timed(value = "driver.list.active", description = "Time taken to list active drivers")
    @Operation(summary = "Get active drivers", description = "Get list of all active drivers")
    public ResponseEntity<ApiResponse<List<DriverResponse>>> getAllActiveDrivers() {

        log.info("Getting all active drivers");
        List<DriverResponse> drivers = driverService.getAllActiveDrivers();

        return ResponseEntity.ok(ApiResponse.success("Active drivers retrieved successfully", drivers));
    }


    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('MODERATOR')")
    @Timed(value = "driver.get.id", description = "Time taken to get driver by ID")
    @Operation(summary = "Get driver by ID", description = "Get driver details by ID")
    public ResponseEntity<ApiResponse<DriverResponse>> getDriverById(
            @Parameter(description = "Driver ID") @PathVariable String id) {

        log.info("Getting driver by ID: {}", id);
        DriverResponse driver = driverService.getDriverById(id);

        return ResponseEntity.ok(ApiResponse.success("Driver retrieved successfully", driver));
    }

    @GetMapping("/name/{driverName}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('MODERATOR')")
    @Timed(value = "driver.get.name", description = "Time taken to get driver by name")
    @Operation(summary = "Get driver by name", description = "Get driver details by driver name")
    public ResponseEntity<ApiResponse<DriverResponse>> getDriverByName(
            @Parameter(description = "Driver name") @PathVariable String driverName) {

        log.info("Getting driver by name: {}", driverName);
        DriverResponse driver = driverService.getDriverByDriverName(driverName);

        return ResponseEntity.ok(ApiResponse.success("Driver retrieved successfully", driver));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('MODERATOR')")
    @Timed(value = "driver.search", description = "Time taken to search drivers")
    @Operation(summary = "Search drivers", description = "Search drivers by name, email, or license number")
    public ResponseEntity<ApiResponse<List<DriverResponse>>> searchDrivers(
            @Parameter(description = "Search query") @RequestParam String q) {

        log.info("Searching drivers with query: {}", q);
        List<DriverResponse> drivers = driverService.searchDrivers(q);

        return ResponseEntity.ok(ApiResponse.success("Driver search completed", drivers));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    @Timed(value = "driver.toggle.status", description = "Time taken to toggle driver status")
    @Operation(summary = "Toggle driver status", description = "Enable or disable a driver")
    public ResponseEntity<ApiResponse<DriverResponse>> toggleDriverStatus(
            @Parameter(description = "Driver ID") @PathVariable String id,
            @RequestBody Map<String, Boolean> request) {

        boolean enabled = request.getOrDefault("enabled", true);
        log.info("Toggling driver status for ID: {} to enabled: {}", id, enabled);

        DriverResponse driver = driverService.toggleDriverStatus(id, enabled);

        return ResponseEntity.ok(ApiResponse.success("Driver status updated successfully", driver));
    }
}