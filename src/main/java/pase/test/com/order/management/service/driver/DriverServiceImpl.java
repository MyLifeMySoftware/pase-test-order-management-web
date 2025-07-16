package pase.test.com.order.management.service.driver;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pase.test.com.database.dto.driver.DriverCreateRequest;
import pase.test.com.database.dto.driver.DriverResponse;
import pase.test.com.database.entity.driver.Driver;
import pase.test.com.database.exception.auth.UserAlreadyExistsException;
import pase.test.com.database.exception.auth.UserNotFoundException;
import pase.test.com.database.repository.driver.DriverRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverServiceImpl implements DriverService {

    private final DriverRepository driverRepository;

    @Transactional
    @Override
    public DriverResponse createDriver(DriverCreateRequest request) {
        log.info("Creating new driver: {}", request.getDriverName());

        if (driverRepository.existsByDriverName(request.getDriverName())) {
            throw new UserAlreadyExistsException("Driver name already exists: " + request.getDriverName());
        }
        if (driverRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new UserAlreadyExistsException("License number already exists: " + request.getLicenseNumber());
        }
        if (driverRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists: " + request.getEmail());
        }
        if (driverRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new UserAlreadyExistsException("Phone number already exists: " + request.getPhoneNumber());
        }

        Driver driver = Driver.builder()
                .driverName(request.getDriverName())
                .licenseNumber(request.getLicenseNumber())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .enabled(true)
                .deleted(false)
                .build();

        driver = driverRepository.save(driver);
        log.info("Driver created successfully: {}", driver.getDriverName());

        return convertToDriverResponse(driver);
    }

    @Override
    public List<DriverResponse> getAllActiveDrivers() {
        log.info("Fetching all active drivers");
        List<Driver> drivers = driverRepository.findAllActiveDrivers();
        return drivers.stream()
                .map(this::convertToDriverResponse)
                .toList();
    }

    @Override
    public DriverResponse getDriverById(String id) {
        log.info("Fetching driver by ID: {}", id);
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Driver not found with ID: " + id));
        return convertToDriverResponse(driver);
    }

    @Override
    public DriverResponse getDriverByDriverName(String driverName) {
        log.info("Fetching driver by name: {}", driverName);
        Driver driver = driverRepository.findByDriverName(driverName)
                .orElseThrow(() -> new UserNotFoundException("Driver not found with name: " + driverName));
        return convertToDriverResponse(driver);
    }

    @Override
    public List<DriverResponse> searchDrivers(String query) {
        log.info("Searching drivers with query: {}", query);
        List<Driver> drivers = driverRepository.searchDrivers(query);
        return drivers.stream()
                .map(this::convertToDriverResponse)
                .toList();
    }

    @Transactional
    @Override
    public DriverResponse toggleDriverStatus(String id, boolean enabled) {
        log.info("Toggling driver status for ID: {} to enabled: {}", id, enabled);

        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Driver not found with ID: " + id));

        driver.setEnabled(enabled);
        driver = driverRepository.save(driver);

        log.info("Driver status updated successfully: {} - enabled: {}", driver.getDriverName(), enabled);
        return convertToDriverResponse(driver);
    }

    @Override
    public Driver getDriverEntityById(String id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Driver not found with ID: " + id));
    }

    private DriverResponse convertToDriverResponse(Driver driver) {
        return DriverResponse.builder()
                .id(driver.getId())
                .driverName(driver.getDriverName())
                .licenseNumber(driver.getLicenseNumber())
                .phoneNumber(driver.getPhoneNumber())
                .email(driver.getEmail())
                .enabled(driver.getEnabled())
                .createdOn(driver.getCreatedOn())
                .lastUpdated(driver.getLastUpdated())
                .modifiedBy(driver.getModifiedBy())
                .build();
    }
}
