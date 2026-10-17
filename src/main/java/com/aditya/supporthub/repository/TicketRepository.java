package com.aditya.supporthub.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.aditya.supporthub.entity.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // Find tickets created by a particular user
    List<Ticket> findByCreatedByEmail(String email);

    // Find a ticket by ID and load its creator
    @Override
    @EntityGraph(attributePaths = {"createdBy"})
    Optional<Ticket> findById(Long id);

    // Find tickets assigned to a particular agent
    List<Ticket> findByAssignedToEmail(String email);

    // Find tickets that have not been assigned to an agent
    List<Ticket> findByAssignedToIsNull();
}