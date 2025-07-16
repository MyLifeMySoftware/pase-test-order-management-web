package pase.test.com.order.management.service.driver;

import java.util.List;
import pase.test.com.database.dto.driver.DriverCreateRequest;
import pase.test.com.database.dto.driver.DriverResponse;
import pase.test.com.database.entity.driver.Driver;

public interface DriverService {

    DriverResponse createDriver(DriverCreateRequest request);

    List<DriverResponse> getAllActiveDrivers();

    DriverResponse getDriverById(String id);

    DriverResponse getDriverByDriverName(String driverName);

    List<DriverResponse> searchDrivers(String query);

    DriverResponse toggleDriverStatus(String id, boolean enabled);

    Driver getDriverEntityById(String id);
}
