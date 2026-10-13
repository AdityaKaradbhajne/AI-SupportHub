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
                        new ResourceNotFoundException(
                                "User not found"));

        // Create the ticket
        Ticket ticket = new Ticket();

        ticket.setTitle(title);
        ticket.setDescription(description);
        ticket.setCategory(category);

        // Set default status and priority
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setPriority(TicketPriority.MEDIUM);

        // Set creation and update timestamps
        LocalDateTime now = LocalDateTime.now();

        ticket.setCreatedAt(now);
        ticket.setUpdatedAt(now);

        // Assign the logged-in user as creator
        ticket.setCreatedBy(user);

        // Save and return the ticket
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

        // Find the ticket
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Ticket not found with id: " + id));

        // Validate the authenticated user's email
        if (userEmail == null || userEmail.isBlank()) {
            throw new ForbiddenException(
                    "You are not allowed to access this ticket");
        }

        // Find the authenticated user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"));

        // Allow Admins and Agents to access any ticket
        if (user.getRole() != null) {

            String role = user.getRole().name();

            if ("ADMIN".equals(role) || "AGENT".equals(role)) {
                return ticket;
            }
        }

        // Check whether the ticket has a creator
        if (ticket.getCreatedBy() == null) {
            throw new ForbiddenException(
                    "This ticket has no assigned creator");
        }

        // Get the ticket creator's email
        String creatorEmail = ticket.getCreatedBy().getEmail();

        // Customers can access only their own tickets
        if (creatorEmail == null
                || !creatorEmail.equalsIgnoreCase(userEmail)) {

            throw new ForbiddenException(
                    "You are not allowed to access this ticket");
        }

        // Return the authorized ticket
        return ticket;
    }

    // Update ticket status
    @Transactional
    public Ticket updateTicketStatus(
            Long id,
            TicketStatus status) {

        // Find the ticket
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Ticket not found with id: " + id));

        // Update status
        ticket.setStatus(status);
        ticket.setUpdatedAt(LocalDateTime.now());

        // Save and return the updated ticket
        return ticketRepository.save(ticket);
    }

    // Update ticket priority
    @Transactional
    public Ticket updateTicketPriority(
            Long id,
            TicketPriority priority) {

        // Find the ticket
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Ticket not found with id: " + id));

        // Update priority
        ticket.setPriority(priority);
        ticket.setUpdatedAt(LocalDateTime.now());

        // Save and return the updated ticket
        return ticketRepository.save(ticket);
    }
}