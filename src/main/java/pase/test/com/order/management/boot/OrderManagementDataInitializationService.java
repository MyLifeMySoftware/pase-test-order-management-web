package pase.test.com.order.management.boot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pase.test.com.database.repository.user.PermissionRepository;
import pase.test.com.database.repository.user.RoleRepository;
import pase.test.com.order.management.service.order.OrderStatusService;
import pase.test.com.order.management.service.order.attachment.AttachmentService;

@Slf4j
@Service
@RequiredArgsConstructor
@Order(2)
public class OrderManagementDataInitializationService implements CommandLineRunner {

    private final OrderStatusService orderStatusService;
    private final AttachmentService attachmentService;
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Starting Order Management data initialization...");
        initializeOrderStatuses();
        initializeAttachmentTypes();

        log.info("Order Management data initialization completed successfully");
    }

    private void initializeOrderStatuses() {
        log.info("Initializing order statuses...");
        orderStatusService.initializeDefaultStatuses();
        log.info("Order statuses initialized successfully");
    }

    private void initializeAttachmentTypes() {
        log.info("Initializing attachment types...");
        attachmentService.initializeDefaultAttachmentTypes();
        log.info("Attachment types initialized successfully");
    }
}