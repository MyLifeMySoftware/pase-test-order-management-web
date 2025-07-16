package pase.test.com.order.management.service.driver;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pase.test.com.database.dto.driver.DriverCreateRequest;
import pase.test.com.database.dto.driver.DriverResponse;
import pase.test.com.database.entity.driver.Driver;
import pase.test.com.database.repository.driver.DriverRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("Driver Service Implementation Tests")
class DriverServiceImplTest {

    @Mock
    private DriverRepository driverRepository;

    @InjectMocks
    private DriverServiceImpl driverService;

    @Test
    @DisplayName("Should create driver successfully")
    void shouldCreateDriverSuccessfully() {
        DriverCreateRequest request = DriverCreateRequest.builder()
                .driverName("John Doe")
                .licenseNumber("ABC123")
                .email("john.doe@example.com")
                .phoneNumber("1234567890")
                .build();

        when(driverRepository.existsByDriverName("John Doe")).thenReturn(false);
        when(driverRepository.existsByLicenseNumber("ABC123")).thenReturn(false);
        when(driverRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(driverRepository.existsByPhoneNumber("1234567890")).thenReturn(false);

        Driver savedDriver = Driver.builder()
                .id("1")
                .driverName("John Doe")
                .licenseNumber("ABC123")
                .email("john.doe@example.com")
                .phoneNumber("1234567890")
                .enabled(true)
                .deleted(false)
                .createdOn(LocalDateTime.now())
                .lastUpdated(LocalDateTime.now())
                .modifiedBy("system")
                .build();

        when(driverRepository.save(any(Driver.class))).thenReturn(savedDriver);

        DriverResponse result = driverService.createDriver(request);

        assertThat(result).isNotNull();
        assertThat(result.getDriverName()).isEqualTo("John Doe");
        assertThat(result.getLicenseNumber()).isEqualTo("ABC123");
        assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(result.getPhoneNumber()).isEqualTo("1234567890");
        assertThat(result.getEnabled()).isTrue();

        verify(driverRepository).existsByDriverName("John Doe");
        verify(driverRepository).existsByLicenseNumber("ABC123");
        verify(driverRepository).existsByEmail("john.doe@example.com");
        verify(driverRepository).existsByPhoneNumber("1234567890");
        verify(driverRepository).save(any(Driver.class));
    }
}