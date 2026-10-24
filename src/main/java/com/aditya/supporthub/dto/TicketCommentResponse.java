package com.aditya.supporthub.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.aditya.supporthub.entity.TicketComment;

public class TicketCommentResponse {

    private Long id;
    private Long ticketId;
    private Long parentCommentId;
    private String authorName;
    private String content;
    private LocalDateTime createdAt;

    private List<TicketCommentResponse> replies = new ArrayList<>();

    public TicketCommentResponse() {
    }

    public TicketCommentResponse(
            Long id,
            Long ticketId,
            Long parentCommentId,
            String authorName,
            String content,
            LocalDateTime createdAt) {

        this.id = id;
        this.ticketId = ticketId;
        this.parentCommentId = parentCommentId;
        this.authorName = authorName;
        this.content = content;
        this.createdAt = createdAt;
    }

    // Convert entity to response DTO
    public static TicketCommentResponse fromEntity(TicketComment comment) {

        Long parentId = comment.getParentComment() != null
                ? comment.getParentComment().getId()
                : null;

        return new TicketCommentResponse(
                comment.getId(),
                comment.getTicket().getId(),
                parentId,
                comment.getAuthor().getName(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public Long getParentCommentId() {
        return parentCommentId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<TicketCommentResponse> getReplies() {
        return replies;
    }

    public void setReplies(List<TicketCommentResponse> replies) {
        this.replies = replies;
    }
}

