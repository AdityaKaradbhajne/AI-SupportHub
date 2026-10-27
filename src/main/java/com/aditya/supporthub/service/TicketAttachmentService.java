package com.aditya.supporthub.service;

import com.aditya.supporthub.entity.Ticket;
import com.aditya.supporthub.entity.TicketAttachment;
import com.aditya.supporthub.entity.User;
import com.aditya.supporthub.repository.TicketAttachmentRepository;
import com.aditya.supporthub.repository.TicketRepository;
import com.aditya.supporthub.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class TicketAttachmentService {


private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

private static final Set<String> ALLOWED_TYPES = Set.of(
        "application/pdf",
        "image/jpeg",
        "image/png",
        "text/plain"
);

private final TicketAttachmentRepository attachmentRepository;
private final TicketRepository ticketRepository;
private final UserRepository userRepository;
private final Path storageDirectory;

public TicketAttachmentService(
        TicketAttachmentRepository attachmentRepository,
        TicketRepository ticketRepository,
        UserRepository userRepository,
        @Value("${app.upload.dir:uploads/ticket-attachments}") String uploadDirectory) {

    this.attachmentRepository = attachmentRepository;
    this.ticketRepository = ticketRepository;
    this.userRepository = userRepository;
    this.storageDirectory = Paths.get(uploadDirectory)
            .toAbsolutePath().normalize();

    try {
        Files.createDirectories(this.storageDirectory);
    } catch (IOException e) {
        throw new IllegalStateException(
                "Could not initialize attachment storage.", e);
    }
}

@Transactional
public TicketAttachment upload(
        Long ticketId, MultipartFile file, String userEmail) {

    if (file == null || file.isEmpty()) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Please select a file.");
    }

    if (file.getSize() > MAX_FILE_SIZE) {
        throw new ResponseStatusException(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "File size must not exceed 5 MB.");
    }

    String originalName = StringUtils.cleanPath(
            file.getOriginalFilename() == null
                    ? "attachment"
                    : file.getOriginalFilename());

    if (originalName.isBlank()
            || originalName.contains("..")
            || originalName.contains("/")
            || originalName.contains("\\")) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Invalid filename.");
    }

    String contentType = file.getContentType();

    if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
        throw new ResponseStatusException(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Only PDF, JPEG, PNG, and plain text files are allowed.");
    }

    Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Ticket not found."));

    User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "User not found."));

    checkTicketAccess(ticket, user);

    String extension = getSafeExtension(originalName);
    String storedName = UUID.randomUUID() + extension;
    Path destination = storageDirectory.resolve(storedName).normalize();

    if (!destination.startsWith(storageDirectory)) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Invalid file path.");
    }

    try {
        Files.copy(file.getInputStream(), destination,
                StandardCopyOption.REPLACE_EXISTING);

        TicketAttachment attachment = new TicketAttachment();
        attachment.setTicket(ticket);
        attachment.setUploadedBy(user);
        attachment.setFileName(originalName);
        attachment.setStoredFileName(storedName);
        attachment.setContentType(contentType);
        attachment.setFileSize(file.getSize());
        attachment.setUploadedAt(LocalDateTime.now());

        return attachmentRepository.save(attachment);

    } catch (IOException e) {
        try {
            Files.deleteIfExists(destination);
        } catch (IOException ignored) {
            // Preserve the original exception.
        }

        throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unable to store the uploaded file.");
    }
}

@Transactional(readOnly = true)
public List<TicketAttachment> getAttachments(
        Long ticketId, String userEmail) {

    Ticket ticket = getTicket(ticketId);
    User user = getUser(userEmail);
    checkTicketAccess(ticket, user);

    return attachmentRepository
            .findByTicketIdOrderByUploadedAtDesc(ticketId);
}

@Transactional(readOnly = true)
public Resource download(
        Long ticketId, Long attachmentId, String userEmail) {

    Ticket ticket = getTicket(ticketId);
    User user = getUser(userEmail);
    checkTicketAccess(ticket, user);

    TicketAttachment attachment = attachmentRepository
            .findByIdAndTicketId(attachmentId, ticketId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Attachment not found."));

    try {
        Path filePath = storageDirectory
                .resolve(attachment.getStoredFileName())
                .normalize();

        if (!filePath.startsWith(storageDirectory)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid file path.");
        }

        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Stored file not found.");
        }

        return resource;

    } catch (MalformedURLException e) {
        throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unable to access the attachment.");
    }
}

@Transactional
public void delete(
        Long ticketId, Long attachmentId, String userEmail) {

    Ticket ticket = getTicket(ticketId);
    User user = getUser(userEmail);
    checkTicketAccess(ticket, user);

    TicketAttachment attachment = attachmentRepository
            .findByIdAndTicketId(attachmentId, ticketId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Attachment not found."));

    if (!"ADMIN".equals(user.getRole().name())
            && !attachment.getUploadedBy().getId().equals(user.getId())) {
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You can delete only your own attachments.");
    }

    try {
        Path filePath = storageDirectory
                .resolve(attachment.getStoredFileName())
                .normalize();

        if (!filePath.startsWith(storageDirectory)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid file path.");
        }

        Files.deleteIfExists(filePath);
        attachmentRepository.delete(attachment);

    } catch (IOException e) {
        throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unable to delete the stored file.");
    }
}

private Ticket getTicket(Long ticketId) {
    return ticketRepository.findById(ticketId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Ticket not found."));
}

private User getUser(String userEmail) {
    return userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "User not found."));
}

private void checkTicketAccess(Ticket ticket, User user) {
    String role = user.getRole().name();

    boolean allowed = switch (role) {
        case "ADMIN" -> true;
        case "CUSTOMER" ->
                ticket.getCreatedBy() != null
                && ticket.getCreatedBy().getId().equals(user.getId());
        case "AGENT" ->
                ticket.getAssignedTo() != null
                && ticket.getAssignedTo().getId().equals(user.getId());
        default -> false;
    };

    if (!allowed) {
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You do not have permission to access this ticket.");
    }
}

private String getSafeExtension(String fileName) {
    int dotIndex = fileName.lastIndexOf('.');

    if (dotIndex < 0) {
        return "";
    }

    String extension = fileName.substring(dotIndex).toLowerCase();

    return switch (extension) {
        case ".pdf", ".jpg", ".jpeg", ".png", ".txt" -> extension;
        default -> "";
    };
}


}
