package com.aditya.supporthub.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

        Ticket ticket = getTicket(ticketId);
        User author = getUser(userEmail);

        checkTicketAccess(ticket, author);

        TicketComment comment = new TicketComment();
        comment.setTicket(ticket);
        comment.setAuthor(author);
        comment.setContent(content.trim());
        comment.setCreatedAt(LocalDateTime.now());

        TicketComment savedComment = commentRepository.save(comment);

        return TicketCommentResponse.fromEntity(savedComment);
    }

    // Reply to an existing top-level comment
    @Transactional
    public TicketCommentResponse replyToComment(
            Long ticketId,
            Long parentCommentId,
            String content,
            String userEmail) {

        validateContent(content);

        Ticket ticket = getTicket(ticketId);
        User author = getUser(userEmail);

        checkTicketAccess(ticket, author);

        TicketComment parentComment = getComment(parentCommentId);

        if (parentComment.getTicket() == null
                || !parentComment.getTicket().getId().equals(ticketId)) {
            throw new IllegalArgumentException(
                    "The parent comment does not belong to this ticket");
        }

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

    // Retrieve comments with replies nested under their parent comments
    @Transactional(readOnly = true)
    public List<TicketCommentResponse> getCommentsByTicketId(
            Long ticketId,
            String userEmail) {

        Ticket ticket = getTicket(ticketId);
        User user = getUser(userEmail);

        checkTicketAccess(ticket, user);

        List<TicketComment> allComments =
                commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);

        Map<Long, TicketCommentResponse> responseMap = new LinkedHashMap<>();

        for (TicketComment comment : allComments) {
            responseMap.put(
                    comment.getId(),
                    TicketCommentResponse.fromEntity(comment));
        }

        List<TicketCommentResponse> topLevelComments = new ArrayList<>();

        for (TicketComment comment : allComments) {

            TicketCommentResponse currentResponse =
                    responseMap.get(comment.getId());

            if (comment.getParentComment() == null) {
                topLevelComments.add(currentResponse);
            } else {
                Long parentId = comment.getParentComment().getId();

                TicketCommentResponse parentResponse = responseMap.get(parentId);

                if (parentResponse != null) {
                    parentResponse.getReplies().add(currentResponse);
                }
            }
        }

        return topLevelComments;
    }

    // Edit an existing comment
    @Transactional
    public TicketCommentResponse updateComment(
            Long ticketId,
            Long commentId,
            String content,
            String userEmail) {

        validateContent(content);

        Ticket ticket = getTicket(ticketId);
        User user = getUser(userEmail);

        checkTicketAccess(ticket, user);

        TicketComment comment = getComment(commentId);

        validateCommentBelongsToTicket(comment, ticketId);
        checkCommentModificationPermission(comment, user);

        comment.setContent(content.trim());
        comment.setUpdatedAt(LocalDateTime.now());

        TicketComment updatedComment = commentRepository.save(comment);

        return TicketCommentResponse.fromEntity(updatedComment);
    }

    // Delete a comment and its replies
    @Transactional
    public void deleteComment(
            Long ticketId,
            Long commentId,
            String userEmail) {

        Ticket ticket = getTicket(ticketId);
        User user = getUser(userEmail);

        checkTicketAccess(ticket, user);

        TicketComment comment = getComment(commentId);

        validateCommentBelongsToTicket(comment, ticketId);
        checkCommentModificationPermission(comment, user);

        // Delete replies first to avoid a foreign-key constraint violation
        commentRepository.deleteRepliesByParentCommentId(commentId);

        // Delete the selected comment
        commentRepository.delete(comment);
    }

    // Validate that the current user can edit or delete this comment
    private void checkCommentModificationPermission(
            TicketComment comment,
            User user) {

        if ("ADMIN".equals(user.getRole().name())) {
            return;
        }

        if (comment.getAuthor() == null
                || !comment.getAuthor().getId().equals(user.getId())) {
            throw new AccessDeniedException(
                    "You can only edit or delete your own comments");
        }
    }

    private void validateCommentBelongsToTicket(
            TicketComment comment,
            Long ticketId) {

        if (comment.getTicket() == null
                || !comment.getTicket().getId().equals(ticketId)) {
            throw new IllegalArgumentException(
                    "The comment does not belong to this ticket");
        }
    }

    private Ticket getTicket(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Ticket not found with id: " + ticketId));
    }

    private User getUser(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));
    }

    private TicketComment getComment(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Comment not found with id: " + commentId));
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

