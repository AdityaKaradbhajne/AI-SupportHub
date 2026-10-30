package com.aditya.supporthub.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aditya.supporthub.entity.Ticket;
import com.aditya.supporthub.entity.User;
import com.aditya.supporthub.exception.ForbiddenException;
import com.aditya.supporthub.exception.ResourceNotFoundException;
import com.aditya.supporthub.repository.TicketRepository;
import com.aditya.supporthub.repository.UserRepository;

@Service
public class TicketAccessService {


private final TicketRepository ticketRepository;
private final UserRepository userRepository;

public TicketAccessService(
        TicketRepository ticketRepository,
        UserRepository userRepository) {

    this.ticketRepository = ticketRepository;
    this.userRepository = userRepository;
}

@Transactional(readOnly = true)
public Ticket validateTicketAccess(
        Long ticketId,
        String userEmail) {

    Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Ticket not found with id: " + ticketId));

    if (userEmail == null || userEmail.isBlank()) {
        throw new ForbiddenException(
                "You are not allowed to access this ticket");
    }

    User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() ->
                    new ResourceNotFoundException("User not found"));

    if (user.getRole() == null) {
        throw new ForbiddenException("User role not found");
    }

    String role = user.getRole().name();

    // ADMIN can access all tickets
    if ("ADMIN".equals(role)) {
        return ticket;
    }

    // CUSTOMER can access only their own tickets
    if ("CUSTOMER".equals(role)) {

        if (ticket.getCreatedBy() != null
                && ticket.getCreatedBy().getEmail() != null
                && ticket.getCreatedBy().getEmail()
                        .equalsIgnoreCase(userEmail)) {

            return ticket;
        }

        throw new ForbiddenException(
                "You can access only your own tickets");
    }

    // AGENT can access only tickets assigned to them
    if ("AGENT".equals(role)) {

        if (ticket.getAssignedTo() != null
                && ticket.getAssignedTo().getEmail() != null
                && ticket.getAssignedTo().getEmail()
                        .equalsIgnoreCase(userEmail)) {

            return ticket;
        }

        throw new ForbiddenException(
                "You can access only tickets assigned to you");
    }

    throw new ForbiddenException(
            "You are not allowed to access this ticket");
}


}
