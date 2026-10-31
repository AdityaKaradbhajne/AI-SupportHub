package com.aditya.supporthub.dto;

import com.aditya.supporthub.entity.TicketCategory;
import com.aditya.supporthub.entity.TicketPriority;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateTicketRequest {

@NotBlank(message = "Title is required")
@Size(
    min = 3,
    max = 100,
    message = "Title must be between 3 and 100 characters"
)
private String title;

@NotBlank(message = "Description is required")
@Size(
    min = 10,
    max = 2000,
    message = "Description must be between 10 and 2000 characters"
)
private String description;

@NotNull(message = "Category is required")
private TicketCategory category;

@NotNull(message = "Priority is required")
private TicketPriority priority;

public CreateTicketRequest() {
}

public String getTitle() {
    return title;
}

public void setTitle(String title) {
    this.title = title;
}

public String getDescription() {
    return description;
}

public void setDescription(String description) {
    this.description = description;
}

public TicketCategory getCategory() {
    return category;
}

public void setCategory(TicketCategory category) {
    this.category = category;
}

public TicketPriority getPriority() {
    return priority;
}

public void setPriority(TicketPriority priority) {
    this.priority = priority;
}


}
