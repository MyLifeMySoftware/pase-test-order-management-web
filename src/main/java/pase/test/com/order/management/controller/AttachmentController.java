package pase.test.com.order.management.controller;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pase.test.com.database.dto.ApiResponse;
import pase.test.com.database.dto.order.OrderResponse;
import pase.test.com.database.dto.order.attachment.AttachmentTypeResponse;
import pase.test.com.database.dto.order.attachment.AttachmentUploadRequest;
import pase.test.com.database.entity.order.attachment.AssignmentAttachment;
import pase.test.com.order.management.service.order.OrderService;
import pase.test.com.order.management.service.order.attachment.AttachmentService;

@Slf4j
@RestController
@RequestMapping("/api/v1/attachments")
@RequiredArgsConstructor
@Tag(name = "Attachment Management", description = "File attachment operations for orders")
@SecurityRequirement(name = "Bearer Authentication")
public class AttachmentController {

    private final AttachmentService attachmentService;
    private final OrderService orderService;

    @PostMapping(value = "/upload/order/{orderId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    @Timed(value = "attachment.upload", description = "Time taken to upload attachment")
    @Operation(summary = "Upload attachment for order",
            description = "Upload a file (PDF or image) and attach it to an order")
    public ResponseEntity<ApiResponse<OrderResponse>> uploadAttachmentForOrder(
            @Parameter(description = "Order ID") @PathVariable String orderId,
            @Valid @ModelAttribute AttachmentUploadRequest request) throws IOException {

        log.info("Uploading attachment for order: {}", orderId);

        AssignmentAttachment attachment = attachmentService.uploadAttachment(request);

        OrderResponse order = orderService.addAttachmentToOrder(orderId, attachment);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Attachment uploaded and added to order successfully", order));
    }

    @GetMapping("/types")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('MODERATOR')")
    @Timed(value = "attachment.types.list", description = "Time taken to list attachment types")
    @Operation(summary = "Get attachment types", description = "Get list of all active attachment types")
    public ResponseEntity<ApiResponse<List<AttachmentTypeResponse>>> getAllAttachmentTypes() {

        log.info("Getting all attachment types");
        List<AttachmentTypeResponse> types = attachmentService.getAllActiveAttachmentTypes();

        return ResponseEntity.ok(ApiResponse.success("Attachment types retrieved successfully", types));
    }
}