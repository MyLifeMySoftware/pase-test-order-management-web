package pase.test.com.order.management.service.order.attachment;

import java.io.IOException;
import java.util.List;
import pase.test.com.database.dto.order.attachment.AttachmentTypeResponse;
import pase.test.com.database.dto.order.attachment.AttachmentUploadRequest;
import pase.test.com.database.entity.order.attachment.AssignmentAttachment;

public interface AttachmentService {

    AssignmentAttachment uploadAttachment(AttachmentUploadRequest request) throws IOException;

    List<AttachmentTypeResponse> getAllActiveAttachmentTypes();

    void initializeDefaultAttachmentTypes();
}
