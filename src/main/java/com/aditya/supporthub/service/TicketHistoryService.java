package com.aditya.supporthub.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aditya.supporthub.dto.TicketHistoryResponse;
import com.aditya.supporthub.entity.Ticket;
import com.aditya.supporthub.entity.TicketHistory;
import com.aditya.supporthub.entity.User;
import com.aditya.supporthub.repository.TicketHistoryRepository;
import com.aditya.supporthub.repository.UserRepository;

@Service
public class TicketHistoryService {

private final TicketHistoryRepository ticketHistoryRepository;
private final TicketAccessService ticketAccessService;
private final UserRepository userRepository;

public TicketHistoryService(
        TicketHistoryRepository ticketHistoryRepository,
        TicketAccessService ticketAccessService,
        UserRepository userRepository) {

    this.ticketHistoryRepository = ticketHistoryRepository;
    this.ticketAccessService = ticketAccessService;
    this.userRepository = userRepository;
}

// Record ticket activity
@Transactional
public TicketHistory recordHistory(
        Ticket ticket,
        User changedBy,
        String action,
        String oldValue,
        String newValue) {

    TicketHistory history = new TicketHistory();

    history.setTicket(ticket);
    history.setChangedBy(changedBy);
    history.setAction(action);
    history.setOldValue(oldValue);
    history.setNewValue(newValue);

    return ticketHistoryRepository.save(history);
}

// Convert entity into DTO
private TicketHistoryResponse convertToResponse(
        TicketHistory history) {

    return new TicketHistoryResponse(
            history.getId(),
            history.getTicket().getId(),
            history.getChangedBy().getId(),
            history.getChangedBy().getName(),
            history.getChangedBy().getEmail(),
            history.getAction(),
            history.getOldValue(),
            history.getNewValue(),
            history.getCreatedAt()
    );
}

// Get history for a particular ticket
@Transactional(readOnly = true)
public List<TicketHistoryResponse> getTicketHistory(
        Long ticketId,
        String userEmail) {

    Ticket ticket = ticketAccessService.validateTicketAccess(
            ticketId, userEmail);

    return ticketHistoryRepository
            .findByTicketIdOrderByCreatedAtDesc(ticket.getId())
            .stream()
            .map(this::convertToResponse)
            .toList();
}

// Get history performed by the logged-in user
@Transactional(readOnly = true)
public List<TicketHistoryResponse> getUserHistory(
        String userEmail) {

    User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() ->
                    new RuntimeException("User not found"));

    return ticketHistoryRepository
            .findByChangedByIdOrderByCreatedAtDesc(user.getId())
            .stream()
            .map(this::convertToResponse)
            .toList();
}


}
