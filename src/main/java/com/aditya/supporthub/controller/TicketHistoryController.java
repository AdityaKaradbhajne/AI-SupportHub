package com.aditya.supporthub.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.aditya.supporthub.dto.TicketHistoryResponse;
import com.aditya.supporthub.service.TicketHistoryService;

@RestController
@RequestMapping("/api")
public class TicketHistoryController {

private final TicketHistoryService ticketHistoryService;

public TicketHistoryController(
        TicketHistoryService ticketHistoryService) {

    this.ticketHistoryService = ticketHistoryService;
}

// Get history of a particular ticket
@GetMapping("/tickets/{ticketId}/history")
public ResponseEntity<List<TicketHistoryResponse>> getTicketHistory(
        @PathVariable Long ticketId,
        Authentication authentication) {

    String email = authentication.getName();

    List<TicketHistoryResponse> history =
            ticketHistoryService.getTicketHistory(
                    ticketId,
                    email
            );

    return ResponseEntity.ok(history);
}

// Get history performed by the logged-in user
@GetMapping("/history/my")
public ResponseEntity<List<TicketHistoryResponse>> getMyHistory(
        Authentication authentication) {

    String email = authentication.getName();

    List<TicketHistoryResponse> history =
            ticketHistoryService.getUserHistory(email);

    return ResponseEntity.ok(history);
}


}
