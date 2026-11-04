
(function () {
    const API_URL = "http://localhost:8080/api";

    const paymentList = document.getElementById("customerPaymentList");
    const paymentMessage = document.getElementById("paymentMessage");

    if (!paymentList) return;

    const token = localStorage.getItem("serveUToken");
    const role = localStorage.getItem("serveURole");

    if (!token || role !== "CUSTOMER") return;

    const headers = {
        Authorization: "Bearer " + token
    };

    const money = value =>
        "₹" + Number(value || 0).toFixed(2);

    function element(tag, text, className) {
        const el = document.createElement(tag);
        el.textContent = text;
        if (className) el.className = className;
        return el;
    }

    function sectionHeading(eyebrow, title, subtitle) {
        const heading = document.createElement("div");
        heading.className = "payment-section-heading";

        heading.append(
            element("p", eyebrow, "eyebrow"),
            element("h3", title),
            element("p", subtitle, "customer-section-subtitle")
        );

        return heading;
    }

    async function loadPayments() {
        paymentList.innerHTML =
            '<p class="payment-loading">Loading your payments...</p>';

        try {
            const [requestsResponse, paymentsResponse] = await Promise.all([
                fetch(API_URL + "/customer/requests", { headers }),
                fetch(API_URL + "/customer/payments", { headers })
            ]);

            const requests = await requestsResponse.json();
            const paymentsData = await paymentsResponse.json();

            if (!requestsResponse.ok || !Array.isArray(requests)) {
                throw new Error("Unable to load completed bookings.");
            }

            if (!paymentsResponse.ok || !paymentsData.success) {
                throw new Error(
                    paymentsData.message || "Unable to load payment history."
                );
            }

            const payments = paymentsData.payments || [];

            paymentList.innerHTML = "";

            const history = document.createElement("div");
            history.className = "payment-history";

            history.appendChild(
                sectionHeading(
                    "YOUR TRANSACTIONS",
                    "Payment History",
                    "A record of your submitted service payments."
                )
            );

            if (payments.length === 0) {
                const empty = document.createElement("div");
                empty.className = "payment-empty";

                empty.append(
                    element("span", "💳", "payment-empty-icon"),
                    element("h4", "No payments yet"),
                    element(
                        "p",
                        "Your completed service payments will appear here."
                    )
                );

                history.appendChild(empty);
            } else {
                payments.forEach(payment => {
                    const card = document.createElement("div");
                    card.className = "payment-history-card";

                    const details = document.createElement("div");
                    details.className = "payment-history-details";

                    details.append(
                        element(
                            "span",
                            "PAYMENT #" + payment.payment_id,
                            "payment-reference"
                        ),
                        element(
                            "h4",
                            "Booking #" + payment.request_id
                        ),
                        element(
                            "p",
                            (payment.payment_method || "UNKNOWN").replace(/_/g, " "),
                            "payment-method-text"
                        )
                    );

                    const amount = document.createElement("div");
                    amount.className = "payment-history-amount";

                    amount.append(
                        element("strong", money(payment.total_amount)),
                        element(
                            "span",
                            (payment.payment_status || "UNKNOWN").replace(/_/g, " "),
                            "payment-status " +
                            (payment.payment_status || "unknown").toLowerCase()
                        )
                    );

                    card.append(details, amount);

                    if (payment.payment_status === "WORKER_CONFIRMED") {
                        const downloadButton = element(
                            "button",
                            "↓ Download Receipt",
                            "customer-download-receipt"
                        );

                        downloadButton.type = "button";

                        downloadButton.addEventListener("click", () => {
                            downloadReceipt(payment, requests);
                        });

                        card.appendChild(downloadButton);
                    }

                    history.appendChild(card);
                });
            }

            paymentList.appendChild(history);

            const paidIds = payments.map(payment =>
                Number(payment.request_id)
            );

            const unpaidRequests = requests.filter(request =>
                request.request_status === "COMPLETED" &&
                !paidIds.includes(Number(request.request_id))
            );

            const checkout = document.createElement("div");
            checkout.className = "payment-checkout";

            checkout.appendChild(
                sectionHeading(
                    "COMPLETE YOUR PAYMENT",
                    "Services Awaiting Payment",
                    "Review the charges and choose how you want to pay."
                )
            );

            if (unpaidRequests.length === 0) {
                const empty = document.createElement("div");
                empty.className = "payment-empty";

                empty.append(
                    element("span", "✓", "payment-empty-icon"),
                    element("h4", "You're all caught up!"),
                    element(
                        "p",
                        "You have no completed services awaiting payment."
                    )
                );

                checkout.appendChild(empty);
            } else {
                unpaidRequests.forEach(request => {
                    checkout.appendChild(createPaymentForm(request));
                });
            }

            paymentList.appendChild(checkout);

        } catch (error) {
            paymentList.innerHTML = "";
            paymentList.appendChild(
                element("p", error.message, "payment-error")
            );
            console.error("Payment loading error:", error);
        }
    }

    function createPaymentForm(request) {
        const card = document.createElement("div");
        card.className = "payment-checkout-card";

        const serviceAmount = Number(request.service_price || 0);
        const inspectionFee = Number(request.inspection_fee || 0);

        const partsAmount =
            request.parts_status === "APPROVED"
                ? Number(request.parts_cost || 0)
                : 0;

        const totalAmount =
            serviceAmount + inspectionFee + partsAmount;

        const top = document.createElement("div");
        top.className = "payment-card-top";

        top.append(
            element("span", "COMPLETED SERVICE", "payment-reference"),
            element("span", "Booking #" + request.request_id, "payment-booking-id")
        );

        card.append(
            top,
            element("h3", request.service_name || "Service payment"),
            element(
                "p",
                "Review your final service charges before submitting payment.",
                "payment-description"
            )
        );

        const summary = document.createElement("div");
        summary.className = "payment-summary";

        summary.appendChild(
            element("h4", "Order Summary")
        );

        function addAmount(label, amount) {
            const row = document.createElement("div");
            row.className = "payment-summary-row";
            row.append(
                element("span", label),
                element("strong", money(amount))
            );
            summary.appendChild(row);
        }

        addAmount("Service amount", serviceAmount);
        addAmount("Inspection fee", inspectionFee);

        if (partsAmount > 0) {
            addAmount("Approved parts", partsAmount);
        }

        const totalRow = document.createElement("div");
        totalRow.className = "payment-total-row";

        totalRow.append(
            element("span", "Total payable"),
            element("strong", money(totalAmount))
        );

        summary.appendChild(totalRow);
        card.appendChild(summary);

        const form = document.createElement("form");
        form.className = "payment-form";

        form.appendChild(
            element("h4", "Choose payment method")
        );

        const methods = document.createElement("div");
        methods.className = "payment-method-options";

        const methodOptions = [
            { value: "UPI", icon: "📱", title: "UPI", description: "Pay using UPI" },
            { value: "CASH", icon: "💵", title: "Cash", description: "Pay the worker directly" },
            { value: "BANK_TRANSFER", icon: "🏦", title: "Bank transfer", description: "Pay through your bank" }
        ];

        let selectedMethod = "";

        methodOptions.forEach(option => {
            const label = document.createElement("label");
            label.className = "payment-method-option";

            const radio = document.createElement("input");
            radio.type = "radio";
            radio.name = "paymentMethod-" + request.request_id;
            radio.value = option.value;
            radio.required = true;

            const content = document.createElement("span");
            content.className = "payment-method-content";

            content.append(
                element("span", option.icon, "payment-method-icon"),
                element("strong", option.title),
                element("small", option.description)
            );

            radio.addEventListener("change", () => {
                selectedMethod = option.value;

                methods.querySelectorAll(".payment-method-option")
                    .forEach(item => item.classList.remove("selected"));

                label.classList.add("selected");
            });

            label.append(radio, content);
            methods.appendChild(label);
        });

        form.appendChild(methods);

        const referenceGroup = document.createElement("div");
        referenceGroup.className = "payment-reference-group";

        const referenceLabel = element(
            "label",
            "Transaction reference (optional)"
        );

        const referenceInput = document.createElement("input");
        referenceInput.type = "text";
        referenceInput.maxLength = 100;
        referenceInput.placeholder = "Enter transaction ID if available";

        referenceGroup.append(referenceLabel, referenceInput);
        form.appendChild(referenceGroup);

        const submitButton = element(
            "button",
            "Submit Payment Details →",
            "payment-submit-button"
        );

        submitButton.type = "submit";
        form.appendChild(submitButton);

        const message = element("", "");
        message.className = "payment-form-message";
        message.setAttribute("role", "status");

        form.appendChild(message);

        form.addEventListener("submit", async event => {
            event.preventDefault();

            if (!selectedMethod) {
                message.textContent = "Please choose a payment method.";
                return;
            }

            submitButton.disabled = true;
            submitButton.textContent = "Submitting...";

            try {
                const response = await fetch(
                    API_URL + "/customer/payment",
                    {
                        method: "POST",
                        headers: {
                            ...headers,
                            "Content-Type": "application/json"
                        },
                        body: JSON.stringify({
                            requestId: Number(request.request_id),
                            paymentMethod: selectedMethod,
                            transactionReference: referenceInput.value.trim()
                        })
                    }
                );

                const result = await response.json();

                if (!response.ok || !result.success) {
                    throw new Error(
                        result.message || "Payment submission failed."
                    );
                }

                showGlobalMessage("Payment details submitted successfully!");

                await loadPayments();

            } catch (error) {
                message.textContent = error.message;
                message.classList.add("error");

                submitButton.disabled = false;
                submitButton.textContent = "Submit Payment Details →";

                console.error("Payment submission error:", error);
            }
        });

        card.appendChild(form);
        return card;
    }

    function downloadReceipt(payment, requests) {
    const request = requests.find(
        item => Number(item.request_id) === Number(payment.request_id)
    );

    const serviceAmount = Number(request?.service_price || 0);
    const inspectionFee = Number(request?.inspection_fee || 0);

    const partsAmount =
        request?.parts_status === "APPROVED"
            ? Number(request.parts_cost || 0)
            : 0;

    const total = Number(payment.total_amount || 0);

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

    const receiptWindow = window.open("", "_blank");

    if (!receiptWindow) {
        showGlobalMessage("Please allow pop-ups to print your receipt.");
        return;
    }

    const charges = `
        <tr><td>Service Amount</td><td>₹${serviceAmount.toFixed(2)}</td></tr>
        <tr><td>Inspection Fee</td><td>₹${inspectionFee.toFixed(2)}</td></tr>
        ${partsAmount > 0
            ? `<tr><td>Approved Parts</td><td>₹${partsAmount.toFixed(2)}</td></tr>`
            : ""}
    `;

    receiptWindow.document.write(`
        <!DOCTYPE html>
        <html>
        <head>
            <title>ServeU Receipt #${safe(payment.payment_id)}</title>
            <style>
                body {
                    font-family: Arial, sans-serif;
                    max-width: 700px;
                    margin: 40px auto;
                    padding: 25px;
                    color: #333;
                }
                h1 { color: #607c57; margin-bottom: 5px; }
                .subtitle { color: #777; }
                hr { border: 0; border-top: 2px solid #607c57; margin: 25px 0; }
                .details p { margin: 12px 0; }
                table { width: 100%; border-collapse: collapse; margin-top: 25px; }
                td { padding: 13px 5px; border-bottom: 1px solid #ddd; }
                td:last-child { text-align: right; }
                .total { font-size: 20px; font-weight: bold; color: #526f4c; }
                .footer { margin-top: 40px; color: #777; font-size: 12px; }
                @media print {
                    body { margin: 15mm auto; }
                    .no-print { display: none; }
                }
            </style>
        </head>
        <body>
            <h1>ServeU</h1>
            <p class="subtitle">SERVICE PAYMENT RECEIPT</p>
            <hr>

            <div class="details">
                <p><strong>Payment ID:</strong> #${safe(payment.payment_id)}</p>
                <p><strong>Booking ID:</strong> #${safe(payment.request_id)}</p>
                <p><strong>Service:</strong> ${safe(request?.service_name || "Service")}</p>
                <p><strong>Payment Method:</strong> ${safe((payment.payment_method || "UNKNOWN").replace(/_/g, " "))}</p>
                <p><strong>Payment Date:</strong> ${safe(paymentDate)}</p>
                <p><strong>Status:</strong> WORKER CONFIRMED</p>
            </div>

            <table>
                ${charges}
                <tr class="total">
                    <td>Total Paid</td>
                    <td>₹${total.toFixed(2)}</td>
                </tr>
            </table>

            <div class="footer">
                <p>Payment confirmed by the service worker.</p>
                <p>This receipt records the payment details submitted through ServeU.</p>
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
    function showGlobalMessage(text) {
        paymentMessage.textContent = text;
        paymentMessage.className = "payment-global-message";
    }

    loadPayments();
})();