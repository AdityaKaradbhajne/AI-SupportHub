package com.aditya.supporthub.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aditya.supporthub.entity.Ticket;
import com.aditya.supporthub.entity.TicketCategory;
import com.aditya.supporthub.entity.TicketPriority;
import com.aditya.supporthub.entity.TicketStatus;
import com.aditya.supporthub.entity.User;
import com.aditya.supporthub.exception.ForbiddenException;
import com.aditya.supporthub.exception.ResourceNotFoundException;
import com.aditya.supporthub.repository.TicketRepository;
import com.aditya.supporthub.repository.UserRepository;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public TicketService(
            TicketRepository ticketRepository,
            UserRepository userRepository) {

        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    // Create a new ticket
    @Transactional
    public Ticket createTicket(
            String title,
            String description,
            TicketCategory category,
            String userEmail) {

        // Find the logged-in user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        // Create the ticket
        Ticket ticket = new Ticket();

        ticket.setTitle(title);
        ticket.setDescription(description);
        ticket.setCategory(category);

        // Set default status and priority
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setPriority(TicketPriority.MEDIUM);

        // Set timestamps
        LocalDateTime now = LocalDateTime.now();

        ticket.setCreatedAt(now);
        ticket.setUpdatedAt(now);

        // Set ticket creator
        ticket.setCreatedBy(user);

        // New tickets remain unassigned until an Admin assigns them
        ticket.setAssignedTo(null);

        return ticketRepository.save(ticket);
    }

    // Get all tickets
    @Transactional(readOnly = true)
    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }

    // Get tickets created by the logged-in user
    @Transactional(readOnly = true)
    public List<Ticket> getMyTickets(String userEmail) {
        return ticketRepository.findByCreatedByEmail(userEmail);
    }

    // Get a ticket by ID with role-based authorization
    @Transactional(readOnly = true)
    public Ticket getTicketById(Long id, String userEmail) {

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
                return ticket;
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

        return ticket;
    }

    // Update ticket status
    @Transactional
    public Ticket updateTicketStatus(
            Long id,
            TicketStatus status) {

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Ticket not found with id: " + id));

        ticket.setStatus(status);
        ticket.setUpdatedAt(LocalDateTime.now());

        return ticketRepository.save(ticket);
    }

    // Update ticket priority
    @Transactional
    public Ticket updateTicketPriority(
            Long id,
            TicketPriority priority) {

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Ticket not found with id: " + id));

        ticket.setPriority(priority);
        ticket.setUpdatedAt(LocalDateTime.now());

        return ticketRepository.save(ticket);
    }

    // Assign a ticket to an Agent
    @Transactional
    public Ticket assignTicketToAgent(
            Long ticketId,
            Long agentId) {

        // Find the ticket
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Ticket not found with id: " + ticketId));

        // Find the Agent
        User agent = userRepository.findById(agentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id: " + agentId));

        // Only users with the AGENT role can be assigned tickets
        if (agent.getRole() == null
                || !"AGENT".equals(agent.getRole().name())) {

            throw new IllegalArgumentException(
                    "The selected user is not an Agent");
        }

        // Assign the Agent
        ticket.setAssignedTo(agent);
        ticket.setUpdatedAt(LocalDateTime.now());

        return ticketRepository.save(ticket);
    }

    // Get tickets assigned to a specific Agent
    @Transactional(readOnly = true)
    public List<Ticket> getAssignedTickets(String agentEmail) {

        // Find the Agent
        User agent = userRepository.findByEmail(agentEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Agent not found"));

        // Verify Agent role
        if (agent.getRole() == null
                || !"AGENT".equals(agent.getRole().name())) {

            throw new ForbiddenException(
                    "Only Agents can view their assigned tickets");
        }

        return ticketRepository.findByAssignedToEmail(agentEmail);
    }

    // Get all unassigned tickets
    @Transactional(readOnly = true)
    public List<Ticket> getUnassignedTickets() {
        return ticketRepository.findByAssignedToIsNull();
    }
}