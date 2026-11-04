
(function () {
    const API_URL = "http://localhost:8080/api";

    const paymentList = document.getElementById("workerPaymentList");
    const summary = document.getElementById("workerPaymentSummary");
    const message = document.getElementById("workerPaymentMessage");

    if (!paymentList || !summary) return;

    const token = localStorage.getItem("serveUToken");
    const role = localStorage.getItem("serveURole");

    if (!token || role !== "WORKER") return;

    const headers = {
        "Authorization": "Bearer " + token
    };

    function money(value) {
        return "₹" + Number(value || 0).toFixed(2);
    }

    async function loadWorkerPayments() {
        paymentList.innerHTML = "<p>Loading payments...</p>";

        try {
            const response = await fetch(
                API_URL + "/worker/payments",
                { headers }
            );

            const data = await response.json();

            if (!response.ok || !data.success) {
                throw new Error(
                    data.message || "Could not load payments."
                );
            }

            const payments = data.payments || [];

            paymentList.innerHTML = "";

            const totalReceived = payments
                .filter(p => p.payment_status === "WORKER_CONFIRMED")
                .reduce((sum, p) => sum + Number(p.total_amount), 0);

            const pending = payments
                .filter(p => p.payment_status === "CUSTOMER_SUBMITTED");

            const pendingAmount = pending.reduce(
                (sum, p) => sum + Number(p.total_amount), 0
            );

            const commissionEarned = payments
                .filter(p => p.payment_status === "WORKER_CONFIRMED")
                .reduce(
                    (sum, p) => sum + Number(p.total_amount) * 0.05,
                    0
                );

            summary.innerHTML = `
                <div class="worker-payment-stat">
                    <span>Payments Received</span>
                    <strong>${money(totalReceived)}</strong>
                </div>

                <div class="worker-payment-stat">
                    <span>Awaiting Confirmation</span>
                    <strong>${money(pendingAmount)}</strong>
                </div>

                <div class="worker-payment-stat">
                    <span>Recorded 5% Commission</span>
                    <strong>${money(commissionEarned)}</strong>
                </div>
            `;

            if (payments.length === 0) {
                paymentList.innerHTML =
                    "<p>No customer payments found yet.</p>";
                return;
            }

            if (status === "WORKER_CONFIRMED") {
                const downloadButton = document.createElement("button");

                downloadButton.type = "button";
                downloadButton.className = "download-worker-statement";
                downloadButton.textContent = "↓ Download Payment Statement";

                downloadButton.addEventListener("click", function () {
                    downloadWorkerStatement(payment);
                });

                card.appendChild(downloadButton);
            }

            payments.forEach(payment => {
                const card = document.createElement("div");
                card.className = "worker-payment-card";

                const status = payment.payment_status || "UNKNOWN";

                card.innerHTML = `
                    <div class="worker-payment-card-header">
                        <div>
                            <span class="payment-reference">
                                PAYMENT #${Number(payment.payment_id)}
                            </span>
                            <h3>Booking #${Number(payment.request_id)}</h3>
                        </div>

                        <span class="worker-payment-status">
                            ${status.replace(/_/g, " ")}
                        </span>
                    </div>

                    <div class="worker-payment-amount">
                        ${money(payment.total_amount)}
                    </div>

                    <p>
                        <strong>Payment method:</strong>
                        ${payment.payment_method.replace(/_/g, " ")}
                    </p>

                    <p>
                        <strong>Payment date:</strong>
                        ${payment.created_at || "Not available"}
                    </p>

                    <p>
                        <strong>ServeU commission (5%):</strong>
                        ${money(Number(payment.total_amount) * 0.05)}
                    </p>

                    <p class="worker-payment-note">
                        ${status === "CUSTOMER_SUBMITTED"
                        ? "Customer has submitted payment details. Confirm only after receiving the money."
                        : status === "WORKER_CONFIRMED"
                            ? "You have confirmed receiving this payment."
                            : status === "DISPUTED"
                                ? "This payment is disputed."
                                : "Payment status requires attention."
                    }
                    </p>

                    ${status === "CUSTOMER_SUBMITTED" ? `
                        <button type="button" class="confirm-worker-payment">
                            Confirm Payment Received
                        </button>
                    ` : ""}
                `;

                if (status === "WORKER_CONFIRMED") {
                    const downloadButton = document.createElement("button");

                    downloadButton.type = "button";
                    downloadButton.className = "download-worker-statement";
                    downloadButton.textContent = "↓ Download Payment Statement";

                    downloadButton.addEventListener("click", function () {
                        downloadWorkerStatement(payment);
                    });

                    card.appendChild(downloadButton);
                }

                const confirmButton = card.querySelector(
                    ".confirm-worker-payment"
                );

                if (confirmButton) {
                    confirmButton.addEventListener("click", function () {
                        confirmPayment(payment.payment_id, confirmButton);
                    });
                }

                paymentList.appendChild(card);
            });

        } catch (error) {
            paymentList.innerHTML = "";
            paymentList.textContent = error.message;
            console.error("Worker payment error:", error);
        }
    }


    function downloadWorkerStatement(payment) {
        const receiptWindow = window.open("", "_blank");

        if (!receiptWindow) {
            message.textContent =
                "Please allow pop-ups to download your statement.";
            return;
        }

        const amount = Number(payment.total_amount || 0);
        const commission = amount * 0.05;
        const workerAmount = amount - commission;

        const paymentDate = payment.created_at
            ? new Date(payment.created_at).toLocaleString("en-IN")
            : "Not available";

        const safe = value =>
            String(value ?? "").replace(/[&<>"']/g, char => ({
                "&": "&amp;",
                "<": "&lt;",
                ">": "&gt;",
                '"': "&quot;",
                "'": "&#39;"
            })[char]);

        receiptWindow.document.write(`
        <!DOCTYPE html>
        <html>
        <head>
            <title>ServeU Payment Statement</title>

            <style>
                body {
                    font-family: Arial, sans-serif;
                    max-width: 700px;
                    margin: 40px auto;
                    padding: 25px;
                    color: #333;
                }

                h1 {
                    color: #607c57;
                    margin-bottom: 5px;
                }

                .subtitle {
                    color: #777;
                }

                hr {
                    border: 0;
                    border-top: 2px solid #607c57;
                    margin: 25px 0;
                }

                .details p {
                    margin: 13px 0;
                }

                table {
                    width: 100%;
                    border-collapse: collapse;
                    margin-top: 25px;
                }

                td {
                    padding: 13px 5px;
                    border-bottom: 1px solid #ddd;
                }

                td:last-child {
                    text-align: right;
                }

                .total {
                    font-weight: bold;
                    font-size: 17px;
                    color: #526f4c;
                }

                .footer {
                    margin-top: 40px;
                    color: #777;
                    font-size: 12px;
                }

                @media print {
                    body {
                        margin: 15mm auto;
                    }

                    .no-print {
                        display: none;
                    }
                }
            </style>
        </head>

        <body>
            <h1>ServeU</h1>
            <p class="subtitle">WORKER PAYMENT STATEMENT</p>

            <hr>

            <div class="details">
                <p><strong>Payment ID:</strong> #${safe(payment.payment_id)}</p>
                <p><strong>Booking ID:</strong> #${safe(payment.request_id)}</p>
                <p><strong>Payment Date:</strong> ${safe(paymentDate)}</p>
                <p><strong>Payment Method:</strong> ${safe(
            (payment.payment_method || "UNKNOWN").replace(/_/g, " ")
        )}</p>
                <p><strong>Payment Status:</strong> WORKER CONFIRMED</p>
            </div>

            <table>
                <tr>
                    <td>Customer Payment</td>
                    <td>₹${amount.toFixed(2)}</td>
                </tr>

                <tr>
                    <td>ServeU Commission (5%)</td>
                    <td>₹${commission.toFixed(2)}</td>
                </tr>

                <tr class="total">
                    <td>Worker Amount After Commission</td>
                    <td>₹${workerAmount.toFixed(2)}</td>
                </tr>
            </table>

            <div class="footer">
                <p>This statement records a payment confirmed by the worker.</p>
                <p>ServeU commission is recorded separately as pending.</p>
            </div>

            <button class="no-print" onclick="window.print()">
                Print / Save as PDF
            </button>

            <script>
                window.onload = function() {
                    window.print();
                };
            <\/script>
        </body>
        </html>
    `);

        receiptWindow.document.close();
    }

    async function confirmPayment(paymentId, button) {
        if (!confirm(
            "Have you actually received this payment from the customer?"
        )) {
            return;
        }

        button.disabled = true;
        button.textContent = "Confirming...";

        try {
            const response = await fetch(
                API_URL + "/worker/confirm-payment",
                {
                    method: "POST",
                    headers: {
                        ...headers,
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        paymentId: Number(paymentId)
                    })
                }
            );

            const result = await response.json();

            if (!response.ok || !result.success) {
                throw new Error(
                    result.message || "Payment confirmation failed."
                );
            }

            message.textContent =
                result.message || "Payment confirmed successfully.";

            await loadWorkerPayments();

        } catch (error) {
            message.textContent = error.message;
            button.disabled = false;
            button.textContent = "Confirm Payment Received";
        }
    }

    loadWorkerPayments();

    // Refresh whenever the worker opens the Payments page.
    document.querySelectorAll(
        '.worker-nav-link[data-page="workerPayments"]'
    ).forEach(button => {
        button.addEventListener("click", loadWorkerPayments);
    });

})();
