package com.aditya.supporthub.dto;

import com.aditya.supporthub.entity.TicketPriority;
import jakarta.validation.constraints.NotNull;

public class UpdateTicketPriorityRequest {

    @NotNull(message = "Ticket priority is required")
    private TicketPriority priority;

    public TicketPriority getPriority() {
        return priority;
    }

    public void setPriority(TicketPriority priority) {
        this.priority = priority;
    }
}