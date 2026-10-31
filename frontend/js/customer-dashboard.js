document.addEventListener("DOMContentLoaded", async function () {

const token = sessionStorage.getItem("token");
const userRole = sessionStorage.getItem("role");

// Check authentication
if (!token) {
    window.location.href = "login.html";
    return;
}

// Check customer role
if (userRole !== "CUSTOMER") {
    alert("You are not authorized to access this dashboard.");
    window.location.href = "login.html";
    return;
}

// Load customer details
loadCustomerDetails();

// Load customer tickets
await loadCustomerTickets(token);

// Logout functionality
document.getElementById("logoutButton").addEventListener("click", function () {
    sessionStorage.clear();
    window.location.href = "login.html";
});

// Navigate to Create Ticket page
document.getElementById("createTicketButton").addEventListener("click", function () {
    window.location.href = "create-ticket.html";
});

});

// Customer Details
function loadCustomerDetails() {

const email = sessionStorage.getItem("email") || "Customer";
const customerName = email.split("@")[0];

document.getElementById("customerName").textContent = customerName;
document.getElementById("customerEmail").textContent = email;

document.getElementById("profileAvatar").textContent =
    customerName.charAt(0).toUpperCase();

}

// Fetch Customer Tickets
async function loadCustomerTickets(token) {

const tableBody = document.getElementById("ticketTableBody");

try {

    const response = await fetch(API_BASE_URL + "/tickets/my", {
        method: "GET",
        headers: {
            "Authorization": "Bearer " + token,
            "Content-Type": "application/json"
        }
    });

    if (!response.ok) {
        throw new Error("Unable to fetch tickets. HTTP " + response.status);
    }

    const tickets = await response.json();

    displayTickets(tickets);
    updateTicketStatistics(tickets);

} catch (error) {

    console.error("Customer Dashboard Error:", error);

    tableBody.innerHTML =
        '<tr><td colspan="6">Unable to load tickets. Please try again.</td></tr>';

}


}

// Display Tickets
function displayTickets(tickets) {


const tableBody = document.getElementById("ticketTableBody");

tableBody.innerHTML = "";

if (!Array.isArray(tickets) || tickets.length === 0) {

    tableBody.innerHTML =
        '<tr><td colspan="6">No tickets found. Create your first ticket!</td></tr>';

    return;
}

tickets.forEach(function (ticket) {

    const row = document.createElement("tr");

    const values = [
        ticket.id,
        ticket.title,
        ticket.category || "General",
        ticket.priority,
        ticket.status,
        ticket.createdAt
            ? new Date(ticket.createdAt).toLocaleDateString()
            : "-"
    ];

    values.forEach(function (value) {

        const cell = document.createElement("td");

        cell.textContent =
            value === null || value === undefined ? "-" : value;

        row.appendChild(cell);

    });

    tableBody.appendChild(row);

});


}

// Update Ticket Statistics
function updateTicketStatistics(tickets) {

const total = tickets.length;

const open = tickets.filter(function (ticket) {
    return ticket.status === "OPEN";
}).length;

const inProgress = tickets.filter(function (ticket) {
    return ticket.status === "IN_PROGRESS";
}).length;

const resolved = tickets.filter(function (ticket) {
    return ticket.status === "RESOLVED";
}).length;

document.getElementById("totalTickets").textContent = total;
document.getElementById("openTickets").textContent = open;
document.getElementById("progressTickets").textContent = inProgress;
document.getElementById("resolvedTickets").textContent = resolved;


}
