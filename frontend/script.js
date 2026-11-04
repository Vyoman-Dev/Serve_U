
const API_URL = "http://localhost:8080/api";

// =========================
// LOAD SERVICE CATEGORIES
// =========================

const categorySelect = document.getElementById("serviceCategory");

async function loadServiceCategories() {
    if (!categorySelect) return;

    try {
        const response = await fetch(API_URL + "/categories");

        if (!response.ok) {
            throw new Error("Could not load categories");
        }

        const categories = await response.json();

        categorySelect.innerHTML =
            '<option value="">Select a service category</option>';

        categories.forEach(function (category) {
            const option = document.createElement("option");
            option.value = category.category_name;
            option.textContent = category.category_name;
            categorySelect.appendChild(option);
        });

    } catch (error) {
        categorySelect.innerHTML =
            '<option value="">Unable to load categories</option>';
        console.error("Category loading error:", error);
    }
}

loadServiceCategories();


// =========================
// REGISTRATION PAGE
// =========================

const accountType = document.getElementById("accountType");
const workerFields = document.getElementById("workerFields");

if (accountType && workerFields) {
    const category = document.getElementById("serviceCategory");
    const experience = document.getElementById("experienceYears");
    const description = document.getElementById("description");

    accountType.addEventListener("change", function () {
        const isWorker = accountType.value === "WORKER";

        workerFields.hidden = !isWorker;
        category.required = isWorker;
        experience.required = isWorker;
        description.required = isWorker;
    });
}


// =========================
// HANDLE REGISTRATION
// =========================

const registerForm = document.getElementById("registerForm");

if (registerForm) {
    registerForm.addEventListener("submit", async function (event) {
        event.preventDefault();

        const message = document.getElementById("registerMessage");
        const button = registerForm.querySelector("button[type='submit']");
        const type = document.getElementById("accountType").value;

        const body = {
            name: document.getElementById("name").value.trim(),
            email: document.getElementById("email").value.trim(),
            password: document.getElementById("password").value,
            phone: document.getElementById("phone").value.trim()
        };

        let endpoint = "";

        if (type === "CUSTOMER") {
            endpoint = "/register/customer";

        } else if (type === "WORKER") {
            endpoint = "/register/worker";

            body.serviceCategory =
                document.getElementById("serviceCategory").value;

            body.experienceYears =
                Number(document.getElementById("experienceYears").value);

            body.description =
                document.getElementById("description").value.trim();

        } else {
            message.textContent = "Please select an account type.";
            return;
        }

        try {
            button.disabled = true;
            message.textContent = "Creating your account...";

            const response = await fetch(API_URL + endpoint, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(body)
            });

            const result = await response.json();

            message.textContent = result.message;

            if (result.success) {
                message.style.color = "green";
                registerForm.reset();

                if (workerFields) {
                    workerFields.hidden = true;
                    document.getElementById("serviceCategory").required = false;
                    document.getElementById("experienceYears").required = false;
                    document.getElementById("description").required = false;
                }

                setTimeout(function () {
                    window.location.href = "login.html";
                }, 1500);

            } else {
                message.style.color = "red";
            }

        } catch (error) {
            message.style.color = "red";
            message.textContent =
                "Cannot connect to ServeU server. Please try again.";

        } finally {
            button.disabled = false;
        }
    });
}


// =========================
// LOGIN PAGE
// =========================

const loginForm = document.getElementById("loginForm");

if (loginForm) {
    loginForm.addEventListener("submit", async function (event) {
        event.preventDefault();

        const message = document.getElementById("loginMessage");
        const button = loginForm.querySelector("button[type='submit']");

        const body = {
            email: document.getElementById("email").value.trim(),
            password: document.getElementById("password").value,
            role: document.getElementById("role").value
        };

        try {
            button.disabled = true;
            message.textContent = "Logging in...";

            const response = await fetch(API_URL + "/login", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(body)
            });

            const result = await response.json();

            if (result.success) {
                localStorage.setItem("serveUToken", result.token);
                localStorage.setItem("serveUUserId", result.userId);
                localStorage.setItem("serveUName", result.name);
                localStorage.setItem("serveURole", result.role);

                message.style.color = "green";
                message.textContent = "Login successful!";


                if (result.role === "CUSTOMER") {
                    window.location.href = "customer-dashboard.html";

                } else if (result.role === "WORKER") {
                    window.location.href = "worker-dashboard.html";

                } else if (result.role === "ADMIN") {
                    window.location.href = "admin-dashboard.html";
                }


            } else {
                message.style.color = "red";
                message.textContent = result.message;
            }

        } catch (error) {
            message.style.color = "red";
            message.textContent = "Cannot connect to ServeU server.";
            console.error(error);

        } finally {
            button.disabled = false;
        }
    });
}


// =========================
// CUSTOMER DASHBOARD
// =========================

const categoryList = document.getElementById("categoryList");

if (categoryList) {
    const token = localStorage.getItem("serveUToken");
    const role = localStorage.getItem("serveURole");
    const name = localStorage.getItem("serveUName");

    if (!token || role !== "CUSTOMER") {
        window.location.href = "login.html";

    } else {
        document.getElementById("welcomeMessage").textContent =
            "Welcome, " + name + "!";

        const categoryIcons = {
            "AC and Appliance Repair": "❄️",
            "Carpentry": "🪚",
            "Computer and IT Support": "💻",
            "Electrical": "💡",
            "Gardening": "🌱",
            "Home Cleaning": "🧹",
            "Painting": "🎨",
            "Pest Control": "🐜",
            "Plumbing": "🔧",
            "Tutoring": "📚"
        };

        // LOAD DASHBOARD CATEGORIES

        async function loadDashboardCategories() {
            try {
                const response = await fetch(API_URL + "/categories");

                if (!response.ok) {
                    throw new Error("Could not load categories");
                }

                const categories = await response.json();
                categoryList.innerHTML = "";

                categories.forEach(function (category) {
                    const card = document.createElement("div");
                    card.className = "category-card";

                    card.setAttribute("role", "button");
                    card.setAttribute("tabindex", "0");
                    card.setAttribute("aria-label",
                        "Find workers for " + category.category_name);

                    const icon = document.createElement("div");
                    icon.className = "category-icon";
                    icon.textContent =
                        categoryIcons[category.category_name] || "🛠️";

                    const title = document.createElement("h3");
                    title.textContent = category.category_name;

                    const description = document.createElement("p");
                    description.textContent =
                        "Find trusted local professionals";

                    card.appendChild(icon);
                    card.appendChild(title);
                    card.appendChild(description);

                    function openCategory() {
                        const selectedCategory = encodeURIComponent(
                            category.category_name
                        );

                        window.location.href =
                            "workers.html?category=" + selectedCategory;
                    }

                    card.addEventListener("click", openCategory);

                    card.addEventListener("keydown", function (event) {
                        if (event.key === "Enter" || event.key === " ") {
                            event.preventDefault();
                            openCategory();
                        }
                    });

                    categoryList.appendChild(card);
                });

            } catch (error) {
                categoryList.textContent = "Unable to load services.";
                console.error("Dashboard category error:", error);
            }
        }

        loadDashboardCategories();


        // =========================
        // LOAD CUSTOMER BOOKINGS
        // =========================

        async function loadCustomerRequests() {
            const requestsList = document.getElementById("customerRequestsList");

            if (!requestsList) return;

            requestsList.innerHTML = "<p>Loading your bookings...</p>";

            try {
                const response = await fetch(API_URL + "/customer/requests", {
                    method: "GET",
                    headers: {
                        "Authorization": "Bearer " + token
                    }
                });

                const requests = await response.json();

                if (!response.ok) {
                    throw new Error(requests.message || "Could not load bookings.");
                }

                requestsList.innerHTML = "";

                if (!Array.isArray(requests) || requests.length === 0) {
                    requestsList.innerHTML =
                        '<p class="customer-empty-message">You have not made any service requests yet. Explore services above to get started.</p>';
                    return;
                }

                const statusMessages = {
                    PENDING: "Your request has been sent. Waiting for the worker to respond.",
                    ACCEPTED: "Your request has been accepted. Your worker is scheduled for your selected appointment.",
                    REJECTED: "The worker has declined your request. You can choose another worker.",
                    WORKER_COMPLETED: "The worker has marked your service as completed. Please confirm whether the work is finished.",
                    COMPLETED: "You confirmed that your service has been completed.",
                    REVISIT_PENDING: "A revisit is required. Waiting for the worker to confirm.",
                    REVISIT_SCHEDULED: "Your worker is scheduled for your approved revisit appointment.",
                    DISPUTED: "Your request is under review."
                };

                requests.forEach(function (request) {
                    const card = document.createElement("div");
                    card.className = "customer-request-card";

                    const status = request.request_status || "UNKNOWN";
                    const partsStatus = request.parts_status || "NOT_SPECIFIED";

                    function addText(tag, text, className) {
                        const element = document.createElement(tag);
                        element.textContent = text;

                        if (className) {
                            element.className = className;
                        }

                        card.appendChild(element);
                        return element;
                    }

                    addText("h3", request.service_name || "Service Request");

                    addText("p", "Request ID: #" + request.request_id);

                    addText(
                        "p",
                        "Worker: " + (request.worker_name || "Not available")
                    );

                    addText(
                        "p",
                        "Problem: " + (request.service_description || "Not provided")
                    );

                    const isRevisitScheduled = request.request_status === "REVISIT_SCHEDULED";

                    const appointmentDate = isRevisitScheduled
                        ? request.revisit_date
                        : request.appointment_date;

                    const appointmentStart = isRevisitScheduled
                        ? request.revisit_time_start
                        : request.preferred_time_start;

                    const appointmentEnd = isRevisitScheduled
                        ? request.revisit_time_end
                        : request.preferred_time_end;

                    addText(
                        "p",
                        (isRevisitScheduled ? "Revisit Appointment: " : "Appointment: ") +
                        (appointmentDate || "Not available") +
                        " | " +
                        (appointmentStart || "") +
                        " - " +
                        (appointmentEnd || "")
                    );

                    addText(
                        "p",
                        "Service price: ₹" + (request.service_price ?? 0)
                    );

                    addText(
                        "p",
                        "Inspection fee: ₹" + (request.inspection_fee ?? 0)
                    );

                    addText(
                        "p",
                        "Parts cost: ₹" + (request.parts_cost ?? 0)
                    );

                    addText(
                        "p",
                        "Parts status: " + partsStatus.replaceAll("_", " ")
                    );

                    addText(
                        "p",
                        "Status: " + status.replaceAll("_", " "),
                        "request-status " + status.toLowerCase()
                    );

                    addText(
                        "p",
                        statusMessages[status] || "Please contact support for an update.",
                        "request-status-message"
                    );


                    // CUSTOMER: Submit a review after service completion

                    if (status === "COMPLETED") {
                        const reviewSection = document.createElement("div");
                        reviewSection.className = "customer-review-section";

                        const heading = document.createElement("h4");
                        heading.textContent = "Rate your experience";
                        reviewSection.appendChild(heading);

                        const ratingLabel = document.createElement("label");
                        ratingLabel.textContent = "Your rating:";

                        const ratingSelect = document.createElement("select");
                        ratingSelect.required = true;

                        const defaultOption = document.createElement("option");
                        defaultOption.value = "";
                        defaultOption.textContent = "Select rating";
                        ratingSelect.appendChild(defaultOption);

                        for (let i = 5; i >= 1; i--) {
                            const option = document.createElement("option");
                            option.value = i;
                            option.textContent = "★".repeat(i) + " (" + i + "/5)";
                            ratingSelect.appendChild(option);
                        }

                        ratingLabel.appendChild(ratingSelect);

                        const commentLabel = document.createElement("label");
                        commentLabel.textContent = "Your feedback (optional):";

                        const commentInput = document.createElement("textarea");
                        commentInput.maxLength = 500;
                        commentInput.placeholder = "Share your experience...";

                        commentLabel.appendChild(commentInput);

                        const submitButton = document.createElement("button");
                        submitButton.textContent = "Submit Review";
                        submitButton.className = "submit-review-button";

                        const message = document.createElement("p");
                        message.className = "review-message";

                        submitButton.addEventListener("click", async function () {
                            const rating = Number(ratingSelect.value);
                            const comment = commentInput.value.trim();

                            if (!rating || rating < 1 || rating > 5) {
                                message.textContent = "Please select a rating from 1 to 5.";
                                return;
                            }

                            submitButton.disabled = true;
                            message.textContent = "Submitting your review...";

                            try {
                                const response = await fetch(
                                    API_URL + "/customer/reviews",
                                    {
                                        method: "POST",
                                        headers: {
                                            "Content-Type": "application/json",
                                            "Authorization": "Bearer " + token
                                        },
                                        body: JSON.stringify({
                                            requestId: request.request_id,
                                            rating: rating,
                                            comment: comment
                                        })
                                    }
                                );

                                const result = await response.json();

                                if (!response.ok || !result.success) {
                                    throw new Error(
                                        result.message || "Could not submit your review."
                                    );
                                }

                                message.textContent = "Thank you! Your review was submitted.";
                                message.style.color = "green";

                                ratingSelect.disabled = true;
                                commentInput.disabled = true;
                                submitButton.disabled = true;
                                submitButton.textContent = "Review Submitted";

                            } catch (error) {
                                message.textContent = error.message;
                                message.style.color = "red";
                                submitButton.disabled = false;
                            }
                        });

                        reviewSection.appendChild(ratingLabel);
                        reviewSection.appendChild(commentLabel);
                        reviewSection.appendChild(submitButton);
                        reviewSection.appendChild(message);

                        card.appendChild(reviewSection);
                    }


                    // CUSTOMER APPROVES OR REJECTS EXTRA PARTS

                    if (partsStatus === "PENDING") {
                        addText(
                            "p",
                            "The worker has proposed additional parts. Please review the cost above."
                        );

                        const actions = document.createElement("div");
                        actions.className = "parts-response-actions";

                        const approveButton = document.createElement("button");
                        approveButton.textContent = "Approve parts";
                        approveButton.className = "approve-parts-button";

                        const rejectButton = document.createElement("button");
                        rejectButton.textContent = "Reject parts";
                        rejectButton.className = "reject-parts-button";

                        async function sendPartsDecision(decision) {
                            approveButton.disabled = true;
                            rejectButton.disabled = true;

                            try {
                                const result = await fetch(
                                    API_URL + "/customer/parts-response",
                                    {
                                        method: "POST",
                                        headers: {
                                            "Content-Type": "application/json",
                                            "Authorization": "Bearer " + token
                                        },
                                        body: JSON.stringify({
                                            requestId: request.request_id,
                                            decision: decision
                                        })
                                    }
                                );

                                const data = await result.json();

                                if (!result.ok || !data.success) {
                                    throw new Error(
                                        data.message || "Could not update parts decision."
                                    );
                                }

                                await loadCustomerRequests();

                            } catch (error) {
                                alert(error.message);
                                approveButton.disabled = false;
                                rejectButton.disabled = false;
                            }
                        }

                        approveButton.addEventListener("click", function () {
                            sendPartsDecision("APPROVED");
                        });

                        rejectButton.addEventListener("click", function () {
                            sendPartsDecision("REJECTED");
                        });

                        actions.appendChild(approveButton);
                        actions.appendChild(rejectButton);
                        card.appendChild(actions);
                    }

                    // CUSTOMER CONFIRMS WORKER COMPLETION

                    if (status === "WORKER_COMPLETED") {
                        const completionSection = document.createElement("div");
                        completionSection.className = "customer-completion-section";

                        const completionNote = document.createElement("p");
                        completionNote.className = "customer-completion-note";
                        completionNote.textContent =
                            "Has the worker finished the service satisfactorily?";

                        const confirmButton = document.createElement("button");
                        confirmButton.textContent = "Confirm Completion";
                        confirmButton.className = "confirm-completion-button";

                        const completionMessage = document.createElement("p");
                        completionMessage.className = "customer-completion-message";

                        confirmButton.addEventListener("click", async function () {
                            const confirmed = window.confirm(
                                "Are you sure the service has been completed?"
                            );

                            if (!confirmed) return;

                            confirmButton.disabled = true;
                            completionMessage.textContent =
                                "Confirming service completion...";

                            try {
                                const result = await fetch(
                                    API_URL + "/customer/confirm-completion",
                                    {
                                        method: "POST",
                                        headers: {
                                            "Content-Type": "application/json",
                                            "Authorization": "Bearer " + token
                                        },
                                        body: JSON.stringify({
                                            requestId: request.request_id
                                        })
                                    }
                                );

                                const data = await result.json();

                                if (!result.ok || !data.success) {
                                    throw new Error(
                                        data.message || "Could not confirm completion."
                                    );
                                }

                                await loadCustomerRequests();

                            } catch (error) {
                                completionMessage.textContent = error.message;
                                confirmButton.disabled = false;
                            }
                        });

                        completionSection.appendChild(completionNote);
                        completionSection.appendChild(confirmButton);
                        completionSection.appendChild(completionMessage);

                        card.appendChild(completionSection);
                    }

                    requestsList.appendChild(card);
                });

            } catch (error) {
                requestsList.innerHTML =
                    '<p class="customer-empty-message">Unable to load your bookings. Please refresh the page.</p>';

                console.error("Customer requests error:", error);
            }
        }


        // =========================
        // LOAD PROPOSED REVISIT APPOINTMENTS
        // =========================


        async function loadProposedRevisits() {
            const revisitsList = document.getElementById("proposedRevisitsList");

            if (!revisitsList) return;

            revisitsList.innerHTML = "<p>Loading proposed appointments...</p>";

            try {
                const response = await fetch(
                    API_URL + "/customer/proposed-revisits",
                    {
                        method: "GET",
                        headers: {
                            "Authorization": "Bearer " + token
                        }
                    }
                );

                const revisits = await response.json();

                if (!response.ok) {
                    throw new Error(
                        revisits.message || "Could not load proposed appointments."
                    );
                }

                revisitsList.innerHTML = "";

                if (!Array.isArray(revisits) || revisits.length === 0) {
                    revisitsList.innerHTML =
                        '<p class="customer-empty-message">You have no proposed revisit appointments.</p>';
                    return;
                }

                revisits.forEach(function (revisit) {
                    const card = document.createElement("div");
                    card.className = "customer-request-card";

                    function addText(tag, text) {
                        const element = document.createElement(tag);
                        element.textContent = text;
                        card.appendChild(element);
                        return element;
                    }

                    addText("h3", revisit.service_name || "Service Revisit");
                    addText("p", "Request ID: #" + revisit.request_id);
                    addText("p", "Worker ID: #" + revisit.worker_id);

                    addText(
                        "p",
                        "Proposed date: " + (revisit.revisit_date || "Not available")
                    );

                    addText(
                        "p",
                        "Proposed time: " +
                        (revisit.revisit_time_start || "") +
                        " - " +
                        (revisit.revisit_time_end || "")
                    );

                    // APPROVE OR REJECT REVISIT PROPOSAL

                    const actions = document.createElement("div");
                    actions.className = "revisit-response-actions";

                    const approveButton = document.createElement("button");
                    approveButton.textContent = "Approve Appointment";
                    approveButton.className = "approve-revisit-button";

                    const rejectButton = document.createElement("button");
                    rejectButton.textContent = "Reject Appointment";
                    rejectButton.className = "reject-revisit-button";

                    const message = document.createElement("p");
                    message.className = "revisit-response-message";

                    async function sendRevisitDecision(approve) {

                        const confirmation = window.confirm(
                            approve
                                ? "Are you sure you want to approve this revisit appointment?"
                                : "Are you sure you want to reject this revisit appointment?"
                        );

                        if (!confirmation) return;

                        approveButton.disabled = true;
                        rejectButton.disabled = true;

                        message.textContent = "Submitting your response...";

                        try {
                            const response = await fetch(
                                API_URL + "/customer/revisit-response",
                                {
                                    method: "POST",
                                    headers: {
                                        "Content-Type": "application/json",
                                        "Authorization": "Bearer " + token
                                    },
                                    body: JSON.stringify({
                                        requestId: revisit.request_id,
                                        approve: approve
                                    })
                                }
                            );

                            const result = await response.json();

                            if (!response.ok || !result.success) {
                                throw new Error(
                                    result.message || "Could not update revisit appointment."
                                );
                            }

                            message.textContent = result.message;
                            message.style.color = "green";

                            // Reload the list to reflect the updated status.
                            await loadProposedRevisits();

                        } catch (error) {
                            message.textContent = error.message;
                            message.style.color = "red";

                            approveButton.disabled = false;
                            rejectButton.disabled = false;
                        }
                    }

                    approveButton.addEventListener("click", function () {
                        sendRevisitDecision(true);
                    });

                    rejectButton.addEventListener("click", function () {
                        sendRevisitDecision(false);
                    });

                    actions.appendChild(approveButton);
                    actions.appendChild(rejectButton);

                    card.appendChild(actions);
                    card.appendChild(message);

                    revisitsList.appendChild(card);
                });

            } catch (error) {
                revisitsList.innerHTML =
                    '<p class="customer-empty-message">Unable to load proposed appointments.</p>';

                console.error("Proposed revisits error:", error);
            }
        }


        loadProposedRevisits();

        loadCustomerRequests();
        //setInterval(loadCustomerRequests, 10000);

        // =========================
        // CUSTOMER REPORTS
        // =========================

        const reportForm = document.getElementById("customerReportForm");
        const reportBookingSelect = document.getElementById("reportRequestId");
        const reportMessage = document.getElementById("reportMessage");
        const customerReportsList = document.getElementById("customerReportsList");

        // LOAD BOOKINGS INTO REPORT DROPDOWN

        async function loadReportBookings() {
            if (!reportBookingSelect) return;

            try {
                const response = await fetch(API_URL + "/customer/requests", {
                    headers: {
                        "Authorization": "Bearer " + token
                    }
                });

                const requests = await response.json();

                if (!response.ok) {
                    throw new Error(requests.message || "Could not load bookings.");
                }

                reportBookingSelect.innerHTML =
                    '<option value="">Select a booking</option>';

                requests.forEach(function (request) {
                    const option = document.createElement("option");
                    option.value = request.request_id;
                    option.textContent =
                        "#" + request.request_id + " - " +
                        (request.service_name || "Service Request") +
                        " (" + (request.request_status || "UNKNOWN") + ")";

                    reportBookingSelect.appendChild(option);
                });

                if (requests.length === 0) {
                    reportBookingSelect.innerHTML =
                        '<option value="">No bookings available</option>';
                }

            } catch (error) {
                console.error("Report booking loading error:", error);
                reportBookingSelect.innerHTML =
                    '<option value="">Unable to load bookings</option>';
            }
        }


        // SUBMIT CUSTOMER REPORT

        if (reportForm) {
            reportForm.addEventListener("submit", async function (event) {
                event.preventDefault();

                const requestId = Number(reportBookingSelect.value);
                const reason = document.getElementById("reportReason").value;
                const description =
                    document.getElementById("reportDescription").value.trim();

                if (!requestId || !reason || !description) {
                    reportMessage.textContent =
                        "Please complete all report fields.";
                    reportMessage.style.color = "red";
                    return;
                }

                const submitButton =
                    reportForm.querySelector("button[type='submit']");

                submitButton.disabled = true;
                reportMessage.textContent = "Submitting your report...";

                try {
                    const response = await fetch(API_URL + "/customer/reports", {
                        method: "POST",
                        headers: {
                            "Content-Type": "application/json",
                            "Authorization": "Bearer " + token
                        },
                        body: JSON.stringify({
                            requestId: requestId,
                            reason: reason,
                            description: description
                        })
                    });

                    const result = await response.json();

                    if (!response.ok || !result.success) {
                        throw new Error(
                            result.message || "Could not submit your report."
                        );
                    }

                    reportMessage.textContent =
                        result.message || "Your report was submitted successfully.";
                    reportMessage.style.color = "green";

                    reportForm.reset();

                    await loadCustomerReports();

                } catch (error) {
                    reportMessage.textContent = error.message;
                    reportMessage.style.color = "red";
                    console.error("Report submission error:", error);

                } finally {
                    submitButton.disabled = false;
                }
            });
        }


        // LOAD CUSTOMER'S SUBMITTED REPORTS

        async function loadCustomerReports() {
            if (!customerReportsList) return;

            customerReportsList.innerHTML =
                "<p>Loading your reports...</p>";

            try {
                const response = await fetch(
                    API_URL + "/customer/my-reports",
                    {
                        method: "GET",
                        headers: {
                            "Authorization": "Bearer " + token
                        }
                    }
                );

                const data = await response.json();

                if (!response.ok) {
                    throw new Error(
                        data.message || "Could not load your reports."
                    );
                }

                const reports = Array.isArray(data)
                    ? data
                    : (data.reports || []);

                customerReportsList.innerHTML = "";

                if (reports.length === 0) {
                    customerReportsList.innerHTML =
                        "<p>You haven't submitted any reports yet.</p>";
                    return;
                }

                reports.forEach(function (report) {
                    const card = document.createElement("div");
                    card.className = "customer-request-card";

                    function addReportText(tag, text) {
                        const element = document.createElement(tag);
                        element.textContent = text;
                        card.appendChild(element);
                    }

                    addReportText(
                        "h4",
                        "Report #" + report.report_id
                    );

                    addReportText(
                        "p",
                        "Booking ID: #" + report.request_id
                    );

                    addReportText(
                        "p",
                        "Reason: " + (report.report_reason || "Not specified")
                    );

                    addReportText(
                        "p",
                        "Description: " +
                        (report.report_description || "Not provided")
                    );

                    const status = report.report_status || "PENDING";

                    addReportText(
                        "p",
                        "Status: " + status.replaceAll("_", " ")
                    );

                    if (report.admin_remarks) {
                        addReportText(
                            "p",
                            "Admin remarks: " + report.admin_remarks
                        );
                    }

                    addReportText(
                        "p",
                        "Submitted: " + (report.created_at || "Date unavailable")
                    );

                    customerReportsList.appendChild(card);
                });

            } catch (error) {
                customerReportsList.innerHTML =
                    "<p>Unable to load your reports. Please refresh the page.</p>";

                console.error("Customer reports loading error:", error);
            }
        }


        // INITIALISE REPORT SECTION

        loadReportBookings();
        loadCustomerReports();

    }
}


// =========================
// CATEGORY SEARCH
// =========================

const serviceSearch = document.getElementById("serviceSearch");

if (serviceSearch) {
    serviceSearch.addEventListener("input", function () {
        const search = serviceSearch.value.toLowerCase();

        document.querySelectorAll(".category-card").forEach(function (card) {
            card.style.display =
                card.textContent.toLowerCase().includes(search) ? "" : "none";
        });
    });
}


/* Customer dashboard page navigation */

document.addEventListener("DOMContentLoaded", function () {

    const navigationButtons = document.querySelectorAll(".customer-nav-link");
    const pages = document.querySelectorAll(".customer-page");

    navigationButtons.forEach(button => {

        button.addEventListener("click", function () {

            const selectedPage = this.dataset.page;

            // Hide every page
            pages.forEach(page => {
                page.classList.remove("active");
            });

            // Remove active state from every navigation button
            navigationButtons.forEach(navButton => {
                navButton.classList.remove("active");
            });

            // Show selected page
            const pageToShow = document.getElementById(selectedPage);

            if (pageToShow) {
                pageToShow.classList.add("active");
                this.classList.add("active");
                window.scrollTo({
                    top: 0,
                    behavior: "smooth"
                });
            }

        });

    });

});


// =========================
// LOGOUT
// =========================

const logoutButton = document.getElementById("logoutButton");

if (logoutButton) {
    logoutButton.addEventListener("click", function () {
        localStorage.removeItem("serveUToken");
        localStorage.removeItem("serveUUserId");
        localStorage.removeItem("serveUName");
        localStorage.removeItem("serveURole");

        window.location.href = "login.html";
    });
}

const passwordInput = document.getElementById("password");
const togglePassword = document.getElementById("togglePassword");

if (passwordInput && togglePassword) {
    togglePassword.addEventListener("pointerdown", function(event) {
        event.preventDefault();
        passwordInput.type = "text";
    });

    function hidePassword() {
        passwordInput.type = "password";
    }

    togglePassword.addEventListener("pointerup", hidePassword);
    togglePassword.addEventListener("pointerleave", hidePassword);
    togglePassword.addEventListener("pointercancel", hidePassword);
    window.addEventListener("blur", hidePassword);
}