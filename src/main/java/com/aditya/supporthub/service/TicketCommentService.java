package com.aditya.supporthub.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aditya.supporthub.dto.TicketCommentResponse;
import com.aditya.supporthub.entity.Ticket;
import com.aditya.supporthub.entity.TicketComment;
import com.aditya.supporthub.entity.User;
import com.aditya.supporthub.exception.ResourceNotFoundException;
import com.aditya.supporthub.repository.TicketCommentRepository;
import com.aditya.supporthub.repository.TicketRepository;
import com.aditya.supporthub.repository.UserRepository;

@Service
public class TicketCommentService {

    private final TicketCommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public TicketCommentService(
            TicketCommentRepository commentRepository,
            TicketRepository ticketRepository,
            UserRepository userRepository) {

        this.commentRepository = commentRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    // Add a new top-level comment to a ticket
    @Transactional
    public TicketCommentResponse addComment(
            Long ticketId,
            String content,
            String userEmail) {

        validateContent(content);

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Ticket not found with id: " + ticketId));

        User author = userRepository.findByEmail(userEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        checkTicketAccess(ticket, author);

        TicketComment comment = new TicketComment();
        comment.setTicket(ticket);
        comment.setAuthor(author);
        comment.setContent(content.trim());
        comment.setCreatedAt(LocalDateTime.now());

        TicketComment savedComment = commentRepository.save(comment);

        return TicketCommentResponse.fromEntity(savedComment);
    }

    // Reply to an existing comment
    @Transactional
    public TicketCommentResponse replyToComment(
            Long ticketId,
            Long parentCommentId,
            String content,
            String userEmail) {

        validateContent(content);

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Ticket not found with id: " + ticketId));

        User author = userRepository.findByEmail(userEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        // Verify the user can access this ticket
        checkTicketAccess(ticket, author);

        TicketComment parentComment = commentRepository.findById(parentCommentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Comment not found with id: " + parentCommentId));

        // A reply must belong to the same ticket
        if (parentComment.getTicket() == null
                || !parentComment.getTicket().getId().equals(ticketId)) {
            throw new IllegalArgumentException(
                    "The parent comment does not belong to this ticket");
        }

        // Keep replies to top-level comments only
        if (parentComment.getParentComment() != null) {
            throw new IllegalArgumentException(
                    "Replies to replies are not supported");
        }

        TicketComment reply = new TicketComment();
        reply.setTicket(ticket);
        reply.setAuthor(author);
        reply.setParentComment(parentComment);
        reply.setContent(content.trim());
        reply.setCreatedAt(LocalDateTime.now());

        TicketComment savedReply = commentRepository.save(reply);

        return TicketCommentResponse.fromEntity(savedReply);
    }

    // Retrieve all comments and replies for a ticket
    @Transactional(readOnly = true)
    public List<TicketCommentResponse> getCommentsByTicketId(
            Long ticketId,
            String userEmail) {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Ticket not found with id: " + ticketId));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        checkTicketAccess(ticket, user);

        return commentRepository
                .findByTicketIdOrderByCreatedAtAsc(ticketId)
                .stream()
                .map(TicketCommentResponse::fromEntity)
                .toList();
    }

    private void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException(
                    "Comment content cannot be empty");
        }
    }

    // Check whether the user can access the ticket
    private void checkTicketAccess(Ticket ticket, User user) {

        String role = user.getRole().name();

        if ("ADMIN".equals(role)) {
            return;
        }

        if ("CUSTOMER".equals(role)) {
            if (ticket.getCreatedBy() != null
                    && ticket.getCreatedBy().getId().equals(user.getId())) {
                return;
            }
        }

        if ("AGENT".equals(role)) {
            if (ticket.getAssignedTo() != null
                    && ticket.getAssignedTo().getId().equals(user.getId())) {
                return;
            }
        }

        throw new AccessDeniedException(
                "You are not authorized to access this ticket");
    }
}

