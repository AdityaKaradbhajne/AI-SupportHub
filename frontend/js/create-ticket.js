document.addEventListener("DOMContentLoaded", function () {

const token = sessionStorage.getItem("token");
const userRole = sessionStorage.getItem("role");

// Check authentication
if (!token) {
    window.location.href = "login.html";
    return;
}

// Only customers can create tickets
if (userRole !== "CUSTOMER") {
    alert("Only customers can create tickets.");
    window.location.href = "login.html";
    return;
}

const ticketForm = document.getElementById("createTicketForm");
const submitButton = document.getElementById("submitButton");
const cancelButton = document.getElementById("cancelButton");
const formMessage = document.getElementById("formMessage");

// Submit ticket
ticketForm.addEventListener("submit", async function (event) {

    event.preventDefault();

    const title = document.getElementById("title").value.trim();
    const category = document.getElementById("category").value.trim();
    const priority = document.getElementById("priority").value;
    const description = document.getElementById("description").value.trim();

    // Validate form
    if (!title || !category || !priority || !description) {
        showMessage("Please fill in all required fields.", "error");
        return;
    }

    if (title.length < 3 || description.length < 10) {
        showMessage("Title must contain at least 3 characters and description at least 10 characters.", "error");
        return;
    }

    // Prepare request data
    const ticketData = {
        title: title,
        category: category,
        priority: priority,
        description: description
    };

    submitButton.disabled = true;
    submitButton.textContent = "Submitting...";

    try {

        const response = await fetch(API_BASE_URL + "/tickets", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": "Bearer " + token
            },
            body: JSON.stringify(ticketData)
        });

        const responseText = await response.text();

        let result = null;

        try {
            result = responseText ? JSON.parse(responseText) : null;
        } catch (error) {
            result = responseText;
        }

        if (!response.ok) {

            const errorMessage =
                typeof result === "string"
                    ? result
                    : result?.message || result?.error || "Unable to create ticket.";

            throw new Error("HTTP " + response.status + ": " + errorMessage);
        }

        showMessage("Ticket created successfully! Redirecting to dashboard...", "success");

        ticketForm.reset();

        setTimeout(function () {
            window.location.href = "customer-dashboard.html";
        }, 1500);

    } catch (error) {

        console.error("Create Ticket Error:", error);

        showMessage(error.message, "error");

    } finally {

        submitButton.disabled = false;
        submitButton.textContent = "Submit Ticket";

    }

});

// Cancel button
cancelButton.addEventListener("click", function () {
    window.location.href = "customer-dashboard.html";
});

// Logout button
document.getElementById("logoutButton").addEventListener("click", function () {
    sessionStorage.clear();
    window.location.href = "login.html";
});

// Display messages
function showMessage(message, type) {

    formMessage.textContent = message;
    formMessage.className = "form-message " + type;

}


});
