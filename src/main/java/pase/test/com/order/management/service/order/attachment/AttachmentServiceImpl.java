package pase.test.com.order.management.service.order.attachment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pase.test.com.database.dto.order.attachment.AttachmentTypeResponse;
import pase.test.com.database.dto.order.attachment.AttachmentUploadRequest;
import pase.test.com.database.entity.order.attachment.AssignmentAttachment;
import pase.test.com.database.entity.order.attachment.AttachmentType;
import pase.test.com.database.exception.order.AttachmentTypeNotFoundException;
import pase.test.com.database.exception.order.InvalidFileTypeException;
import pase.test.com.database.repository.order.attachment.AssignmentAttachmentRepository;
import pase.test.com.database.repository.order.attachment.AttachmentTypeRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

    private final AttachmentTypeRepository attachmentTypeRepository;
    private final AssignmentAttachmentRepository assignmentAttachmentRepository;

    @Value("${app.upload.directory:${java.io.tmpdir}/pase-uploads}")
    private String uploadDirectory;

    @Transactional
    @Override
    public AssignmentAttachment uploadAttachment(AttachmentUploadRequest request) throws IOException {
        log.info("Uploading attachment: {}", request.getFile().getOriginalFilename());

        AttachmentType attachmentType = attachmentTypeRepository.findByTypeLabel(request.getAttachmentTypeLabel())
                .orElseThrow(() -> new AttachmentTypeNotFoundException(
                        "Attachment type not found: " + request.getAttachmentTypeLabel()));

        String originalFilename = request.getFile().getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);

        if (!isValidExtension(fileExtension, attachmentType.getAllowedExtensions())) {
            throw new InvalidFileTypeException(
                    "Invalid file type. Allowed extensions: " + attachmentType.getAllowedExtensions());
        }

        Path uploadPath = Paths.get(uploadDirectory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String uniqueFilename = UUID.randomUUID().toString() + "_" + originalFilename;
        Path filePath = uploadPath.resolve(uniqueFilename);

        Files.copy(request.getFile().getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        AssignmentAttachment attachment = AssignmentAttachment.builder()
                .attachmentType(attachmentType)
                .fileName(originalFilename)
                .filePath(filePath.toString())
                .fileSizeBytes(request.getFile().getSize())
                .enabled(true)
                .deleted(false)
                .build();

        attachment = assignmentAttachmentRepository.save(attachment);
        log.info("Attachment uploaded successfully: {}", attachment.getId());

        return attachment;
    }

    @Override
    public List<AttachmentTypeResponse> getAllActiveAttachmentTypes() {
        log.info("Fetching all active attachment types");
        List<AttachmentType> types = attachmentTypeRepository.findAllActiveTypes();
        return types.stream()
                .map(this::convertToAttachmentTypeResponse)
                .toList();
    }

    @Transactional
    @Override
    public void initializeDefaultAttachmentTypes() {
        log.info("Initializing default attachment types");

        if (!attachmentTypeRepository.existsByTypeLabel("PDF")) {
            AttachmentType pdfType = AttachmentType.builder()
                    .typeLabel("PDF")
                    .allowedExtensions(".pdf")
                    .enabled(true)
                    .deleted(false)
                    .build();
            attachmentTypeRepository.save(pdfType);
            log.info("Created default attachment type: PDF");
        }

        if (!attachmentTypeRepository.existsByTypeLabel("IMAGE")) {
            AttachmentType imageType = AttachmentType.builder()
                    .typeLabel("IMAGE")
                    .allowedExtensions(".png,.jpg,.jpeg")
                    .enabled(true)
                    .deleted(false)
                    .build();
            attachmentTypeRepository.save(imageType);
            log.info("Created default attachment type: IMAGE");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }

    private boolean isValidExtension(String fileExtension, String allowedExtensions) {
        if (allowedExtensions == null || fileExtension == null) {
            return false;
        }
        return allowedExtensions.toLowerCase().contains(fileExtension.toLowerCase());
    }

    private AttachmentTypeResponse convertToAttachmentTypeResponse(AttachmentType type) {
        return AttachmentTypeResponse.builder()
                .id(type.getId())
                .typeLabel(type.getTypeLabel())
                .allowedExtensions(type.getAllowedExtensions())
                .enabled(type.getEnabled())
                .createdOn(type.getCreatedOn())
                .lastUpdated(type.getLastUpdated())
                .build();
    }
}
