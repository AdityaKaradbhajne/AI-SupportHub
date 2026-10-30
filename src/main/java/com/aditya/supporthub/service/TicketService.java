package com.aditya.supporthub.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aditya.supporthub.dto.TicketResponse;
import com.aditya.supporthub.entity.Ticket;
import com.aditya.supporthub.entity.TicketCategory;
import com.aditya.supporthub.entity.TicketPriority;
import com.aditya.supporthub.entity.TicketStatus;
import com.aditya.supporthub.entity.User;
import com.aditya.supporthub.exception.ForbiddenException;
import com.aditya.supporthub.exception.ResourceNotFoundException;
import com.aditya.supporthub.repository.TicketCommentRepository;
import com.aditya.supporthub.repository.TicketRepository;
import com.aditya.supporthub.repository.UserRepository;

@Service
public class TicketService {

private final TicketRepository ticketRepository;
private final UserRepository userRepository;
private final TicketCommentRepository ticketCommentRepository;
private final TicketHistoryService ticketHistoryService;
private final TicketAccessService ticketAccessService;

public TicketService(
        TicketRepository ticketRepository,
        UserRepository userRepository,
        TicketCommentRepository ticketCommentRepository,
        TicketHistoryService ticketHistoryService,
        TicketAccessService ticketAccessService) {

    this.ticketRepository = ticketRepository;
    this.userRepository = userRepository;
    this.ticketCommentRepository = ticketCommentRepository;
    this.ticketHistoryService = ticketHistoryService;
    this.ticketAccessService = ticketAccessService;
}

// Create a new ticket
@Transactional
public TicketResponse createTicket(
        String title,
        String description,
        TicketCategory category,
        String userEmail) {

    User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() ->
                    new ResourceNotFoundException("User not found"));

    Ticket ticket = new Ticket();

    ticket.setTitle(title);
    ticket.setDescription(description);
    ticket.setCategory(category);
    ticket.setStatus(TicketStatus.OPEN);
    ticket.setPriority(TicketPriority.MEDIUM);

    LocalDateTime now = LocalDateTime.now();

    ticket.setCreatedAt(now);
    ticket.setUpdatedAt(now);
    ticket.setCreatedBy(user);
    ticket.setAssignedTo(null);

    Ticket savedTicket = ticketRepository.save(ticket);

    // Record ticket creation
    ticketHistoryService.recordHistory(
            savedTicket,
            user,
            "TICKET_CREATED",
            null,
            "Ticket created"
    );

    return TicketResponse.fromEntity(savedTicket);
}

// Get all tickets
@Transactional(readOnly = true)
public List<TicketResponse> getAllTickets() {

    return ticketRepository.findAll()
            .stream()
            .map(TicketResponse::fromEntity)
            .toList();
}

// Get tickets created by the logged-in user
@Transactional(readOnly = true)
public List<TicketResponse> getMyTickets(String userEmail) {

    return ticketRepository.findByCreatedByEmail(userEmail)
            .stream()
            .map(TicketResponse::fromEntity)
            .toList();
}

// Get a ticket by ID with centralized authorization
@Transactional(readOnly = true)
public TicketResponse getTicketById(
        Long id,
        String userEmail) {

    Ticket ticket = ticketAccessService.validateTicketAccess(
            id,
            userEmail
    );

    return TicketResponse.fromEntity(ticket);
}

// Update ticket status
@Transactional
public TicketResponse updateTicketStatus(
        Long id,
        TicketStatus status,
        String userEmail) {

    Ticket ticket = ticketRepository.findById(id)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Ticket not found with id: " + id));

    User changedBy = userRepository.findByEmail(userEmail)
            .orElseThrow(() ->
                    new ResourceNotFoundException("User not found"));

    TicketStatus oldStatus = ticket.getStatus();

    ticket.setStatus(status);
    ticket.setUpdatedAt(LocalDateTime.now());

    Ticket updatedTicket = ticketRepository.save(ticket);

    // Record status change only when the value changes
    if (oldStatus != status) {

        ticketHistoryService.recordHistory(
                updatedTicket,
                changedBy,
                "STATUS_UPDATED",
                oldStatus != null ? oldStatus.name() : null,
                status != null ? status.name() : null
        );
    }

    return TicketResponse.fromEntity(updatedTicket);
}

// Update ticket priority
@Transactional
public TicketResponse updateTicketPriority(
        Long id,
        TicketPriority priority,
        String userEmail) {

    Ticket ticket = ticketRepository.findById(id)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Ticket not found with id: " + id));

    User changedBy = userRepository.findByEmail(userEmail)
            .orElseThrow(() ->
                    new ResourceNotFoundException("User not found"));

    TicketPriority oldPriority = ticket.getPriority();

    ticket.setPriority(priority);
    ticket.setUpdatedAt(LocalDateTime.now());

    Ticket updatedTicket = ticketRepository.save(ticket);

    // Record priority change only when the value changes
    if (oldPriority != priority) {

        ticketHistoryService.recordHistory(
                updatedTicket,
                changedBy,
                "PRIORITY_UPDATED",
                oldPriority != null ? oldPriority.name() : null,
                priority != null ? priority.name() : null
        );
    }

    return TicketResponse.fromEntity(updatedTicket);
}

// Assign a ticket to an Agent
@Transactional
public TicketResponse assignTicketToAgent(
        Long ticketId,
        Long agentId,
        String userEmail) {

    Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Ticket not found with id: " + ticketId));

    User admin = userRepository.findByEmail(userEmail)
            .orElseThrow(() ->
                    new ResourceNotFoundException("User not found"));

    User agent = userRepository.findById(agentId)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "User not found with id: " + agentId));

    if (agent.getRole() == null
            || !"AGENT".equals(agent.getRole().name())) {

        throw new IllegalArgumentException(
                "The selected user is not an Agent");
    }

    User oldAgent = ticket.getAssignedTo();

    ticket.setAssignedTo(agent);
    ticket.setUpdatedAt(LocalDateTime.now());

    Ticket updatedTicket = ticketRepository.save(ticket);

    // Record agent assignment only when assignment changes
    if (oldAgent == null
            || !oldAgent.getId().equals(agent.getId())) {

        String oldValue = oldAgent != null
                ? oldAgent.getEmail()
                : "Unassigned";

        ticketHistoryService.recordHistory(
                updatedTicket,
                admin,
                "AGENT_ASSIGNED",
                oldValue,
                agent.getEmail()
        );
    }

    return TicketResponse.fromEntity(updatedTicket);
}

// Get tickets assigned to a specific Agent
@Transactional(readOnly = true)
public List<TicketResponse> getAssignedTickets(
        String agentEmail) {

    User agent = userRepository.findByEmail(agentEmail)
            .orElseThrow(() ->
                    new ResourceNotFoundException("Agent not found"));

    if (agent.getRole() == null
            || !"AGENT".equals(agent.getRole().name())) {

        throw new ForbiddenException(
                "Only Agents can view their assigned tickets");
    }

    return ticketRepository.findByAssignedToEmail(agentEmail)
            .stream()
            .map(TicketResponse::fromEntity)
            .toList();
}

// Get all unassigned tickets
@Transactional(readOnly = true)
public List<TicketResponse> getUnassignedTickets() {

    return ticketRepository.findByAssignedToIsNull()
            .stream()
            .map(TicketResponse::fromEntity)
            .toList();
}

// Delete a ticket and its associated comments
@Transactional
public void deleteTicket(Long id) {

    Ticket ticket = ticketRepository.findById(id)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Ticket not found with id: " + id));

    // Delete comments before deleting the ticket
    ticketCommentRepository.deleteByTicketId(id);

    // Delete the ticket
    ticketRepository.delete(ticket);
}


}
