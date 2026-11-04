
const API_URL = "http://localhost:8080/api";

// Get the saved admin login details
const token = localStorage.getItem("serveUToken");
const role = localStorage.getItem("serveURole");

// Check admin authentication
if (!token || role !== "ADMIN") {
    alert("Please login using your admin account.");
    window.location.href = "login.html";
}

// Common API request function
async function apiRequest(endpoint, method = "GET", body = null) {
    const options = {
        method: method,
        headers: {
            "Content-Type": "application/json",
            "Authorization": "Bearer " + token
        }
    };

    if (body !== null) {
        options.body = JSON.stringify(body);
    }

    const response = await fetch(API_URL + endpoint, options);

    const data = await response.json();

    if (!response.ok) {
        throw new Error(data.message || "Request failed");
    }

    return data;
}

// Handle different API response formats
function getList(data, key) {
    if (Array.isArray(data)) return data;
    if (Array.isArray(data[key])) return data[key];
    if (Array.isArray(data.data)) return data.data;
    return [];
}

// Navigation
document.querySelectorAll(".nav-link").forEach(button => {
    button.addEventListener("click", () => {
        document.querySelectorAll(".nav-link").forEach(item => {
            item.classList.remove("active");
        });

        document.querySelectorAll(".dashboard-section").forEach(section => {
            section.classList.remove("active");
        });

        button.classList.add("active");

        const sectionId = button.dataset.section;
        document.getElementById(sectionId).classList.add("active");
    });
});

// Load dashboard statistics
async function loadStats() {
    try {
        const data = await apiRequest("/admin/stats");

        const stats = data.stats || data.data || data;

        document.getElementById("totalCustomers").textContent =
            stats.customers ?? 0;

        document.getElementById("totalWorkers").textContent =
            stats.workers ?? 0;

        document.getElementById("pendingWorkers").textContent =
            stats.pending_workers ?? 0;

        document.getElementById("pendingReports").textContent =
            stats.pending_reports ?? 0;

    } catch (error) {
        console.error("Statistics error:", error);
    }
}

// Load customers
async function loadCustomers() {
    const container = document.getElementById("customerList");

    try {
        const data = await apiRequest("/admin/customers");
        const customers = getList(data, "customers");

        if (customers.length === 0) {
            container.textContent = "No customers found.";
            return;
        }

        container.innerHTML = `
            <table>
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Name</th>
                        <th>Email</th>
                        <th>Phone</th>
                        <th>Status</th>
                    </tr>
                </thead>
                <tbody>
                    ${customers.map(customer => `
                        <tr>
                            <td>${customer.user_id}</td>
                            <td>${customer.name}</td>
                            <td>${customer.email}</td>
                            <td>${customer.phone || "-"}</td>
                            <td>${customer.account_status}</td>
                        </tr>
                    `).join("")}
                </tbody>
            </table>
        `;

    } catch (error) {
        container.textContent = error.message;
    }
}

// Load workers
async function loadWorkers() {
    const container = document.getElementById("workerList");

    try {
        const data = await apiRequest("/admin/workers");
        const workers = getList(data, "workers");

        if (workers.length === 0) {
            container.textContent = "No workers found.";
            return;
        }

        container.innerHTML = `
            <table>
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Name</th>
                        <th>Category</th>
                        <th>Experience</th>
                        <th>Verification</th>
                        <th>Action</th>
                    </tr>
                </thead>
                <tbody>
                    ${workers.map(worker => `
                        <tr>
                            <td>${worker.worker_id}</td>
                            <td>${worker.name}</td>
                            <td>${worker.service_category}</td>
                            <td>${worker.experience_years} years</td>
                            <td>${worker.verification_status}</td>
                            <td>
                                ${worker.verification_status === "PENDING" ? `
                                    <button onclick="verifyWorker(${worker.worker_id}, 'APPROVED')">
                                        Approve
                                    </button>
                                    <button onclick="verifyWorker(${worker.worker_id}, 'REJECTED')">
                                        Reject
                                    </button>
                                ` : "-"}
                            </td>
                        </tr>
                    `).join("")}
                </tbody>
            </table>
        `;

    } catch (error) {
        container.textContent = error.message;
    }
}

// Approve or reject a worker
async function verifyWorker(workerId, decision) {
    const remarks = prompt("Enter admin remarks:");

    if (remarks === null) return;

    if (!remarks.trim()) {
        alert("Please enter remarks.");
        return;
    }

    try {
        await apiRequest("/admin/verify-worker", "POST", {
            workerId: workerId,
            decision: decision,
            adminRemarks: remarks
        });

        alert("Worker verification updated.");
        loadWorkers();
        loadStats();

    } catch (error) {
        alert(error.message);
    }
}

// Load reports
async function loadReports() {
    const container = document.getElementById("reportList");

    try {
        const data = await apiRequest("/admin/reports");
        const reports = getList(data, "reports");

        if (reports.length === 0) {
            container.textContent = "No reports found.";
            return;
        }

        container.innerHTML = `
            <table>
                <thead>
                    <tr>
                        <th>Report ID</th>
                        <th>Reason</th>
                        <th>Reporter</th>
                        <th>Reported User</th>
                        <th>Status</th>
                        <th>Action</th>
                    </tr>
                </thead>
                <tbody>
                    ${reports.map(report => `
                        <tr>
                            <td>${report.report_id}</td>
                            <td>${report.report_reason}</td>
                            <td>${report.reporter_name}</td>
                            <td>${report.reported_user_name}</td>
                            <td>${report.report_status}</td>
                            <td>
                                ${["PENDING", "UNDER_REVIEW"].includes(report.report_status) ? `
                                    <button onclick="updateReport(${report.report_id}, 'UNDER_REVIEW')">
                                        Review
                                    </button>
                                    <button onclick="updateReport(${report.report_id}, 'RESOLVED')">
                                        Resolve
                                    </button>
                                    <button onclick="updateReport(${report.report_id}, 'DISMISSED')">
                                        Dismiss
                                    </button>
                                ` : "-"}
                            </td>
                        </tr>
                    `).join("")}
                </tbody>
            </table>
        `;

    } catch (error) {
        container.textContent = error.message;
    }
}

// Update report status
async function updateReport(reportId, decision) {
    const remarks = prompt("Enter admin remarks:");

    if (remarks === null) return;

    if (!remarks.trim()) {
        alert("Please enter remarks.");
        return;
    }

    try {
        await apiRequest("/admin/update-report", "POST", {
            reportId: reportId,
            decision: decision,
            adminRemarks: remarks
        });

        alert("Report updated successfully.");
        loadReports();
        loadStats();

    } catch (error) {
        alert(error.message);
    }
}

// Logout
document.getElementById("logoutBtn").addEventListener("click", () => {
    localStorage.removeItem("serveUToken");
    localStorage.removeItem("serveUUserId");
    localStorage.removeItem("serveUName");
    localStorage.removeItem("serveURole");

    window.location.href = "login.html";
});

// Load all dashboard data
loadStats();
loadCustomers();
loadWorkers();
loadReports();
