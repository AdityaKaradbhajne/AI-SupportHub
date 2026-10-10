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
    public Ticket createTicket(
            String title,
            String description,
            TicketCategory category,
            String userEmail) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        Ticket ticket = new Ticket();

        ticket.setTitle(title);
        ticket.setDescription(description);
        ticket.setCategory(category);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setPriority(TicketPriority.MEDIUM);
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());
        ticket.setCreatedBy(user);

        return ticketRepository.save(ticket);
    }

    // Get all tickets
    @Transactional(readOnly = true)
    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }

    // Get tickets created by a specific user
    @Transactional(readOnly = true)
    public List<Ticket> getMyTickets(String userEmail) {
        return ticketRepository.findByCreatedByEmail(userEmail);
    }

    // Get a ticket by ID with ownership authorization
    @Transactional(readOnly = true)
    public Ticket getTicketById(Long id, String userEmail) {

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Ticket not found with id: " + id));

        if (ticket.getCreatedBy() == null) {
            throw new ForbiddenException(
                    "This ticket has no assigned creator");
        }

        if (!ticket.getCreatedBy().getEmail().equals(userEmail)) {
            throw new ForbiddenException(
                    "You are not allowed to access this ticket");
        }

        return ticket;
    }

    // Update ticket status
    @Transactional
    public Ticket updateTicketStatus(Long id, TicketStatus status) {

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
    public Ticket updateTicketPriority(Long id, TicketPriority priority) {

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Ticket not found with id: " + id));

        ticket.setPriority(priority);
        ticket.setUpdatedAt(LocalDateTime.now());

        return ticketRepository.save(ticket);
    }
}