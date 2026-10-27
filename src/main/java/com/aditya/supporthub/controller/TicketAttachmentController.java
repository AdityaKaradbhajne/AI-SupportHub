package com.aditya.supporthub.controller;

import com.aditya.supporthub.entity.TicketAttachment;
import com.aditya.supporthub.service.TicketAttachmentService;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/tickets/{ticketId}/attachments")
public class TicketAttachmentController {


private final TicketAttachmentService attachmentService;

public TicketAttachmentController(
        TicketAttachmentService attachmentService) {
    this.attachmentService = attachmentService;
}

@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<AttachmentResponse> upload(
        @PathVariable Long ticketId,
        @RequestParam("file") MultipartFile file,
        Authentication authentication) {

    TicketAttachment attachment = attachmentService.upload(
            ticketId, file, authentication.getName());

    return ResponseEntity.status(HttpStatus.CREATED)
            .body(AttachmentResponse.fromEntity(attachment));
}

@GetMapping
public ResponseEntity<List<AttachmentResponse>> getAttachments(
        @PathVariable Long ticketId,
        Authentication authentication) {

    List<AttachmentResponse> attachments = attachmentService
            .getAttachments(ticketId, authentication.getName())
            .stream()
            .map(AttachmentResponse::fromEntity)
            .toList();

    return ResponseEntity.ok(attachments);
}

@GetMapping("/{attachmentId}")
public ResponseEntity<Resource> download(
        @PathVariable Long ticketId,
        @PathVariable Long attachmentId,
        Authentication authentication) {

    Resource resource = attachmentService.download(
            ticketId, attachmentId, authentication.getName());

    String filename = resource.getFilename() == null
            ? "attachment"
            : resource.getFilename();

    return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                    ContentDisposition.attachment()
                            .filename(filename, StandardCharsets.UTF_8)
                            .build().toString())
            .body(resource);
}

@DeleteMapping("/{attachmentId}")
public ResponseEntity<Void> delete(
        @PathVariable Long ticketId,
        @PathVariable Long attachmentId,
        Authentication authentication) {

    attachmentService.delete(
            ticketId, attachmentId, authentication.getName());

    return ResponseEntity.noContent().build();
}

public static class AttachmentResponse {

    private Long id;
    private Long ticketId;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private String uploadedBy;
    private java.time.LocalDateTime uploadedAt;

    public AttachmentResponse() {
    }

    public static AttachmentResponse fromEntity(
            TicketAttachment attachment) {

        AttachmentResponse response = new AttachmentResponse();
        response.setId(attachment.getId());
        response.setTicketId(attachment.getTicket().getId());
        response.setFileName(attachment.getFileName());
        response.setContentType(attachment.getContentType());
        response.setFileSize(attachment.getFileSize());
        response.setUploadedBy(
                attachment.getUploadedBy().getEmail());
        response.setUploadedAt(attachment.getUploadedAt());

        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public java.time.LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(
            java.time.LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}


}
