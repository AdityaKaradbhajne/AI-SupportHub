package com.aditya.supporthub.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aditya.supporthub.dto.TicketCommentResponse;
import com.aditya.supporthub.service.TicketCommentService;

@RestController
@RequestMapping("/api/tickets/{ticketId}/comments")
public class TicketCommentController {

    private final TicketCommentService commentService;

    public TicketCommentController(TicketCommentService commentService) {
        this.commentService = commentService;
    }

    // Add a new top-level comment to a ticket
    @PostMapping
    public ResponseEntity<TicketCommentResponse> addComment(
            @PathVariable Long ticketId,
            @RequestBody CommentRequest request,
            Authentication authentication) {

        String userEmail = authentication.getName();

        TicketCommentResponse response = commentService.addComment(
                ticketId,
                request.getContent(),
                userEmail);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Reply to an existing comment
    @PostMapping("/{commentId}/replies")
    public ResponseEntity<TicketCommentResponse> replyToComment(
            @PathVariable Long ticketId,
            @PathVariable Long commentId,
            @RequestBody CommentRequest request,
            Authentication authentication) {

        String userEmail = authentication.getName();

        TicketCommentResponse response = commentService.replyToComment(
                ticketId,
                commentId,
                request.getContent(),
                userEmail);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Get all comments and replies for a ticket
    @GetMapping
    public ResponseEntity<List<TicketCommentResponse>> getComments(
            @PathVariable Long ticketId,
            Authentication authentication) {

        String userEmail = authentication.getName();

        List<TicketCommentResponse> comments =
                commentService.getCommentsByTicketId(
                        ticketId,
                        userEmail);

        return ResponseEntity.ok(comments);
    }

    // Request body for creating a comment or reply
    public static class CommentRequest {

        private String content;

        public CommentRequest() {
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
