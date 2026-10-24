package com.aditya.supporthub.dto;

import java.time.LocalDateTime;

import com.aditya.supporthub.entity.Ticket;

public record TicketResponse(
        Long id,
        String title,
        String description,
        String category,
        String status,
        String priority,
        Long createdById,
        String createdByName,
        String createdByEmail,
        Long assignedToId,
        String assignedToName,
        String assignedToEmail,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Double aiConfidence,
        String aiSuggestedResponse
) {

    public static TicketResponse fromEntity(Ticket ticket) {

        Long createdById = null;
        String createdByName = null;
        String createdByEmail = null;

        if (ticket.getCreatedBy() != null) {
            createdById = ticket.getCreatedBy().getId();
            createdByName = ticket.getCreatedBy().getName();
            createdByEmail = ticket.getCreatedBy().getEmail();
        }

        Long assignedToId = null;
        String assignedToName = null;
        String assignedToEmail = null;

        if (ticket.getAssignedTo() != null) {
            assignedToId = ticket.getAssignedTo().getId();
            assignedToName = ticket.getAssignedTo().getName();
            assignedToEmail = ticket.getAssignedTo().getEmail();
        }

        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getCategory() != null
                        ? ticket.getCategory().toString()
                        : null,
                ticket.getStatus() != null
                        ? ticket.getStatus().toString()
                        : null,
                ticket.getPriority() != null
                        ? ticket.getPriority().toString()
                        : null,
                createdById,
                createdByName,
                createdByEmail,
                assignedToId,
                assignedToName,
                assignedToEmail,
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getAiConfidence(),
                ticket.getAiSuggestedResponse()
        );
    }
}
