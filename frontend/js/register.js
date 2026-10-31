document.getElementById("registerForm").addEventListener("submit", async function (event) {

    event.preventDefault();

    // Get form values
    const name = document.getElementById("name").value.trim();
    const email = document.getElementById("email").value.trim();
    const password = document.getElementById("password").value;
    const confirmPassword = document.getElementById("confirmPassword").value;

    const button = document.getElementById("registerButton");
    const message = document.getElementById("message");

    // Reset message
    message.className = "message";
    message.textContent = "";

    // Validate name
    if (name.length < 2 || name.length > 100) {
        message.textContent = "Name must be between 2 and 100 characters.";
        message.className = "message error";
        return;
    }

    // Validate password
    if (password.length < 8 || password.length > 100) {
        message.textContent = "Password must be between 8 and 100 characters.";
        message.className = "message error";
        return;
    }

    // Confirm password
    if (password !== confirmPassword) {
        message.textContent = "Passwords do not match.";
        message.className = "message error";
        return;
    }

    // Disable button
    button.disabled = true;
    button.textContent = "Creating Account...";

    try {

        // Call Spring Boot registration API
        const response = await fetch(`${API_BASE_URL}/auth/register`, {

            method: "POST",

            headers: {
                "Content-Type": "application/json"
            },

            body: JSON.stringify({
                name: name,
                email: email,
                password: password
            })

        });

        // Read backend response
        const responseText = await response.text();

        let data = {};

        try {
            data = responseText ? JSON.parse(responseText) : {};
        } catch {
            data = {};
        }

        // Handle registration errors
        if (!response.ok) {

            throw new Error(
                data.message ||
                data.error ||
                "Registration failed. Please try again."
            );

        }

        // Registration successful
        message.textContent =
            "Account created successfully! Redirecting to login...";

        message.className = "message success";

        // Redirect to login page
        setTimeout(() => {
            window.location.href = "login.html";
        }, 1500);

    } catch (error) {

        console.error("Registration Error:", error);

        message.textContent =
            error.message || "Something went wrong.";

        message.className = "message error";

    } finally {

        button.disabled = false;
        button.textContent = "Create Account";

    }

});

