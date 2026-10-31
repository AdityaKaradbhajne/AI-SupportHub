package com.aditya.supporthub.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.aditya.supporthub.dto.CreateTicketRequest;
import com.aditya.supporthub.dto.TicketResponse;
import com.aditya.supporthub.dto.UpdateTicketPriorityRequest;
import com.aditya.supporthub.dto.UpdateTicketStatusRequest;
import com.aditya.supporthub.service.TicketService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

private final TicketService ticketService;

public TicketController(TicketService ticketService) {
    this.ticketService = ticketService;
}

// Create a new ticket
@PostMapping
public ResponseEntity<TicketResponse> createTicket(
        @Valid @RequestBody CreateTicketRequest request,
        Authentication authentication) {

    String userEmail = authentication.getName();

    TicketResponse ticket = ticketService.createTicket(
            request.getTitle(),
            request.getDescription(),
            request.getCategory(),
            userEmail
    );

    return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ticket);
}

// Get tickets created by the logged-in customer
@GetMapping("/my")
public ResponseEntity<List<TicketResponse>> getMyTickets(
        Authentication authentication) {

    String userEmail = authentication.getName();

    return ResponseEntity.ok(
            ticketService.getMyTickets(userEmail)
    );
}

// Get all tickets - AGENT and ADMIN only
@PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
@GetMapping
public ResponseEntity<List<TicketResponse>> getAllTickets() {

    return ResponseEntity.ok(
            ticketService.getAllTickets()
    );
}

// Get tickets assigned to the logged-in Agent
@PreAuthorize("hasRole('AGENT')")
@GetMapping("/assigned")
public ResponseEntity<List<TicketResponse>> getAssignedTickets(
        Authentication authentication) {

    String agentEmail = authentication.getName();

    return ResponseEntity.ok(
            ticketService.getAssignedTickets(agentEmail)
    );
}

// Get all unassigned tickets - ADMIN only
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/unassigned")
public ResponseEntity<List<TicketResponse>> getUnassignedTickets() {

    return ResponseEntity.ok(
            ticketService.getUnassignedTickets()
    );
}

// Assign a ticket to an Agent - ADMIN only
@PreAuthorize("hasRole('ADMIN')")
@PatchMapping("/{id}/assign/{agentId}")
public ResponseEntity<TicketResponse> assignTicketToAgent(
        @PathVariable Long id,
        @PathVariable Long agentId,
        Authentication authentication) {

    String userEmail = authentication.getName();

    TicketResponse assignedTicket =
            ticketService.assignTicketToAgent(
                    id,
                    agentId,
                    userEmail
            );

    return ResponseEntity.ok(assignedTicket);
}

// Get a ticket by ID
@GetMapping("/{id}")
public ResponseEntity<TicketResponse> getTicketById(
        @PathVariable Long id,
        Authentication authentication) {

    String userEmail = authentication.getName();

    return ResponseEntity.ok(
            ticketService.getTicketById(id, userEmail)
    );
}

// Update ticket status - AGENT and ADMIN only
@PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
@PatchMapping("/{id}/status")
public ResponseEntity<TicketResponse> updateTicketStatus(
        @PathVariable Long id,
        @Valid @RequestBody UpdateTicketStatusRequest request,
        Authentication authentication) {

    String userEmail = authentication.getName();

    TicketResponse updatedTicket =
            ticketService.updateTicketStatus(
                    id,
                    request.getStatus(),
                    userEmail
            );

    return ResponseEntity.ok(updatedTicket);
}

// Update ticket priority - AGENT and ADMIN only
@PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
@PatchMapping("/{id}/priority")
public ResponseEntity<TicketResponse> updateTicketPriority(
        @PathVariable Long id,
        @Valid @RequestBody UpdateTicketPriorityRequest request,
        Authentication authentication) {

    String userEmail = authentication.getName();

    TicketResponse updatedTicket =
            ticketService.updateTicketPriority(
                    id,
                    request.getPriority(),
                    userEmail
            );

    return ResponseEntity.ok(updatedTicket);
}

// Delete a ticket - ADMIN only
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteTicket(
        @PathVariable Long id) {

    ticketService.deleteTicket(id);

    return ResponseEntity.noContent().build();
}


}
