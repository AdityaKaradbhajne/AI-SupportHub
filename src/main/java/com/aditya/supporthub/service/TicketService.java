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

    public TicketService(
            TicketRepository ticketRepository,
            UserRepository userRepository,
            TicketCommentRepository ticketCommentRepository) {

        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.ticketCommentRepository = ticketCommentRepository;
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

    // Get a ticket by ID with role-based authorization
    @Transactional(readOnly = true)
    public TicketResponse getTicketById(
            Long id,
            String userEmail) {

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Ticket not found with id: " + id));

        if (userEmail == null || userEmail.isBlank()) {
            throw new ForbiddenException(
                    "You are not allowed to access this ticket");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        // Admins and Agents can access any ticket
        if (user.getRole() != null) {

            String role = user.getRole().name();

            if ("ADMIN".equals(role) || "AGENT".equals(role)) {
                return TicketResponse.fromEntity(ticket);
            }
        }

        // Customers can access only their own tickets
        if (ticket.getCreatedBy() == null) {
            throw new ForbiddenException(
                    "This ticket has no assigned creator");
        }

        String creatorEmail = ticket.getCreatedBy().getEmail();

        if (creatorEmail == null
                || !creatorEmail.equalsIgnoreCase(userEmail)) {

            throw new ForbiddenException(
                    "You are not allowed to access this ticket");
        }

        return TicketResponse.fromEntity(ticket);
    }

    // Update ticket status
    @Transactional
    public TicketResponse updateTicketStatus(
            Long id,
            TicketStatus status) {

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Ticket not found with id: " + id));

        ticket.setStatus(status);
        ticket.setUpdatedAt(LocalDateTime.now());

        Ticket updatedTicket = ticketRepository.save(ticket);

        return TicketResponse.fromEntity(updatedTicket);
    }

    // Update ticket priority
    @Transactional
    public TicketResponse updateTicketPriority(
            Long id,
            TicketPriority priority) {

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Ticket not found with id: " + id));

        ticket.setPriority(priority);
        ticket.setUpdatedAt(LocalDateTime.now());

        Ticket updatedTicket = ticketRepository.save(ticket);

        return TicketResponse.fromEntity(updatedTicket);
    }

    // Assign a ticket to an Agent
    @Transactional
    public TicketResponse assignTicketToAgent(
            Long ticketId,
            Long agentId) {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Ticket not found with id: " + ticketId));

        User agent = userRepository.findById(agentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id: " + agentId));

        if (agent.getRole() == null
                || !"AGENT".equals(agent.getRole().name())) {

            throw new IllegalArgumentException(
                    "The selected user is not an Agent");
        }

        ticket.setAssignedTo(agent);
        ticket.setUpdatedAt(LocalDateTime.now());

        Ticket updatedTicket = ticketRepository.save(ticket);

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

