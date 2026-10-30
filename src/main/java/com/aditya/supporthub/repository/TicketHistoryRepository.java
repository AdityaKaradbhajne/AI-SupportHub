package com.aditya.supporthub.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.aditya.supporthub.entity.TicketHistory;

@Repository
public interface TicketHistoryRepository extends JpaRepository<TicketHistory, Long> {

// Get complete history of a particular ticket
List<TicketHistory> findByTicketIdOrderByCreatedAtDesc(Long ticketId);

// Get history created by a particular user
List<TicketHistory> findByChangedByIdOrderByCreatedAtDesc(Long userId);


}
