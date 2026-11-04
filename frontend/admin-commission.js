
let allCommissionRecords = [];

// Load commission summary
async function loadCommissionSummary() {
    try {
        const data = await apiRequest("/admin/commission-summary");

        document.getElementById("pendingCommission").textContent =
            "₹" + Number(data.pending_amount || 0).toFixed(2);

        document.getElementById("collectedCommission").textContent =
            "₹" + Number(data.collected_amount || 0).toFixed(2);

        document.getElementById("totalCommissions").textContent =
            data.total_commissions || 0;

    } catch (error) {
        console.error("Commission summary error:", error);
    }
}


// Load all commission records
async function loadCommissions() {
    const container = document.getElementById("commissionList");

    container.textContent = "Loading commissions...";

    try {
        const data = await apiRequest("/admin/commissions");

        allCommissionRecords = Array.isArray(data)
            ? data
            : data.commissions || data.data || [];

        displayCommissions();

    } catch (error) {
        container.textContent = error.message;
        console.error("Commission loading error:", error);
    }
}


// Display commissions according to selected filter
function displayCommissions() {
    const container = document.getElementById("commissionList");
    const filter = document.getElementById("commissionFilter").value;

    const commissions = filter === "ALL"
        ? allCommissionRecords
        : allCommissionRecords.filter(item =>
            item.commission_status === filter
        );

    if (commissions.length === 0) {
        container.textContent = "No commissions found for this filter.";
        return;
    }

    container.innerHTML = `
        <table>
            <thead>
                <tr>
                    <th>Commission ID</th>
                    <th>Worker</th>
                    <th>Payment ID</th>
                    <th>Customer Payment</th>
                    <th>Rate</th>
                    <th>Commission</th>
                    <th>Status</th>
                    <th>Collection Date</th>
                    <th>Payment Method</th>
                    <th>Action</th>
                </tr>
            </thead>

            <tbody>
                ${commissions.map(item => `
                    <tr>
                        <td>${item.commission_id}</td>
                        <td>${escapeCommissionText(item.worker_name)}</td>
                        <td>${item.payment_id}</td>
                        <td>₹${Number(item.total_amount || 0).toFixed(2)}</td>
                        <td>${Number(item.commission_rate || 0)}%</td>
                        <td>₹${Number(item.commission_amount || 0).toFixed(2)}</td>
                        <td>
                            <span class="commission-status ${
                                item.commission_status === "PAID"
                                    ? "paid" : "pending"
                            }">
                                ${escapeCommissionText(item.commission_status)}
                            </span>
                        </td>
                        <td>${formatCommissionDate(item.collected_at)}</td>
                        <td>${escapeCommissionText(item.payment_method)}</td>
                        <td>
                            ${item.commission_status === "PENDING"
                                ? `<button class="collect-commission-btn"
                                    onclick="collectCommission(${Number(item.commission_id)})">
                                    Mark as Collected
                                   </button>`
                                : "Collected"}
                        </td>
                    </tr>
                `).join("")}
            </tbody>
        </table>
    `;
}


// Safely display text from the database
function escapeCommissionText(value) {
    return String(value ?? "-").replace(/[&<>"']/g, character => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;"
    })[character]);
}


// Format collection date
function formatCommissionDate(value) {
    if (!value) return "Not collected";

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return escapeCommissionText(value);
    }

    return date.toLocaleString("en-IN");
}


// Mark commission as collected
async function collectCommission(commissionId) {
    const confirmed = confirm(
        "Have you actually received this commission payment from the worker?"
    );

    if (!confirmed) return;

    try {
        const result = await apiRequest(
            "/admin/collect-commission",
            "POST",
            { commissionId: commissionId }
        );

        alert(result.message || "Commission updated successfully.");

        await loadCommissions();
        await loadCommissionSummary();

    } catch (error) {
        alert(error.message);
    }
}


// Handle filter changes
document.getElementById("commissionFilter").addEventListener("change", () => {
    displayCommissions();
});


// Initial loading
loadCommissions();
loadCommissionSummary();
