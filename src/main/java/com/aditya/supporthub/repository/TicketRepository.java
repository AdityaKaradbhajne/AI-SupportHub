package com.aditya.supporthub.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.aditya.supporthub.entity.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByCreatedByEmail(String email);

    @Override
    @EntityGraph(attributePaths = {"createdBy"})
    Optional<Ticket> findById(Long id);
}