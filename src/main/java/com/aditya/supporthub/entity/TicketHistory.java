package com.aditya.supporthub.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "ticket_history")
public class TicketHistory {

@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "ticket_id", nullable = false)
private Ticket ticket;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "changed_by", nullable = false)
private User changedBy;

@Column(nullable = false)
private String action;

@Column(name = "old_value", columnDefinition = "TEXT")
private String oldValue;

@Column(name = "new_value", columnDefinition = "TEXT")
private String newValue;

@Column(name = "created_at", nullable = false)
private LocalDateTime createdAt;

@PrePersist
protected void onCreate() {
    createdAt = LocalDateTime.now();
}

public Long getId() {
    return id;
}

public Ticket getTicket() {
    return ticket;
}

public void setTicket(Ticket ticket) {
    this.ticket = ticket;
}

public User getChangedBy() {
    return changedBy;
}

public void setChangedBy(User changedBy) {
    this.changedBy = changedBy;
}

public String getAction() {
    return action;
}

public void setAction(String action) {
    this.action = action;
}

public String getOldValue() {
    return oldValue;
}

public void setOldValue(String oldValue) {
    this.oldValue = oldValue;
}

public String getNewValue() {
    return newValue;
}

public void setNewValue(String newValue) {
    this.newValue = newValue;
}

public LocalDateTime getCreatedAt() {
    return createdAt;
}


}
