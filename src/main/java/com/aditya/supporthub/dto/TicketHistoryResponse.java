package com.aditya.supporthub.dto;

import java.time.LocalDateTime;

public class TicketHistoryResponse {

    private Long id;
    private Long ticketId;
    private Long changedById;
    private String changedByName;
    private String changedByEmail;
    private String action;
    private String oldValue;
    private String newValue;
    private LocalDateTime createdAt;

    public TicketHistoryResponse(
            Long id,
            Long ticketId,
            Long changedById,
            String changedByName,
            String changedByEmail,
            String action,
            String oldValue,
            String newValue,
            LocalDateTime createdAt) {

        this.id = id;
        this.ticketId = ticketId;
        this.changedById = changedById;
        this.changedByName = changedByName;
        this.changedByEmail = changedByEmail;
        this.action = action;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public Long getChangedById() {
        return changedById;
    }

    public String getChangedByName() {
        return changedByName;
    }

    public String getChangedByEmail() {
        return changedByEmail;
    }

    public String getAction() {
        return action;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}