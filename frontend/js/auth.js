// Decode JWT payload
function decodeJWT(token) {

    try {

        const payload = token.split(".")[1];

        const base64 = payload
            .replace(/-/g, "+")
            .replace(/_/g, "/");

        const decoded = atob(base64);

        return JSON.parse(
            decodeURIComponent(
                Array.from(decoded)
                    .map(char =>
                        "%" + char.charCodeAt(0)
                            .toString(16)
                            .padStart(2, "0")
                    )
                    .join("")
            )
        );

    } catch (error) {

        throw new Error("Unable to read authentication token.");

    }
}


// Login Form Submission
document.getElementById("loginForm").addEventListener("submit", async function (event) {

    event.preventDefault();

    const email = document.getElementById("email").value.trim();
    const password = document.getElementById("password").value;

    const button = document.getElementById("loginButton");
    const message = document.getElementById("message");

    // Disable button during login
    button.disabled = true;
    button.textContent = "Signing in...";

    message.className = "message";
    message.textContent = "";

    try {

        // Send login request to Spring Boot backend
        const response = await fetch(`${API_BASE_URL}/auth/login`, {

            method: "POST",

            headers: {
                "Content-Type": "application/json"
            },

            body: JSON.stringify({
                email: email,
                password: password
            })

        });

        // Read plain JWT string from backend
        const token = (await response.text()).trim();

        // Handle unsuccessful login
        if (!response.ok) {

            let errorMessage = "Invalid email or password.";

            try {

                const errorData = JSON.parse(token);

                errorMessage =
                    errorData.message ||
                    errorData.error ||
                    errorMessage;

            } catch {
                // Keep default error message
            }

            throw new Error(errorMessage);

        }

        // Validate JWT structure
        if (!token || token.split(".").length !== 3) {

            throw new Error("Invalid JWT token received from server.");

        }

        // Decode JWT claims
        const user = decodeJWT(token);

        // Read role from JWT
        const role = user.role;

        // Dashboard mapping
        const dashboards = {

            CUSTOMER: "customer-dashboard.html",
            AGENT: "agent-dashboard.html",
            ADMIN: "admin-dashboard.html"

        };

        // Validate role
        if (!dashboards[role]) {

            throw new Error("Invalid user role. Please contact administrator.");

        }

        // Store authentication details
        sessionStorage.setItem("token", token);

        sessionStorage.setItem("userId", user.userId);

        sessionStorage.setItem("email", user.sub);

        sessionStorage.setItem("role", role);

        // Display success message
        message.textContent = "Login successful! Redirecting...";

        message.className = "message success";

        // Redirect according to user role
        setTimeout(() => {

            window.location.href = dashboards[role];

        }, 1200);

    } catch (error) {

        console.error("Login Error:", error);

        message.textContent = error.message;

        message.className = "message error";

    } finally {

        button.disabled = false;

        button.textContent = "Sign In";

    }

});

