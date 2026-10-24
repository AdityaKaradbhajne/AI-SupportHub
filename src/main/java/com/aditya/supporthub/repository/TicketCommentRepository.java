package com.aditya.supporthub.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.aditya.supporthub.entity.TicketComment;

@Repository
public interface TicketCommentRepository
        extends JpaRepository<TicketComment, Long> {

    // Find all comments associated with a ticket
    List<TicketComment> findByTicketIdOrderByCreatedAtAsc(Long ticketId);

    // Find replies to a specific comment
    List<TicketComment> findByParentCommentIdOrderByCreatedAtAsc(Long parentCommentId);

    // Delete all comments associated with a ticket
    @Modifying
    @Query("DELETE FROM TicketComment c WHERE c.ticket.id = :ticketId")
    void deleteByTicketId(@Param("ticketId") Long ticketId);
}

