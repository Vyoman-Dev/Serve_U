const workerToken = localStorage.getItem("serveUToken");
const workerRole = localStorage.getItem("serveURole");
const workerName = localStorage.getItem("serveUName");

if (!workerToken || workerRole !== "WORKER") {
    alert("Please log in as a worker first.");
    window.location.href = "login.html";
}

document.getElementById("workerWelcome").textContent =
    "Welcome, " + (workerName || "Worker") + "!";

const requestList = document.getElementById("workerRequestList");

const reviewsList = document.getElementById("workerReviewsList");
async function loadWorkerReviews() {
    try {
        reviewsList.innerHTML = "<p>Loading customer reviews...</p>";

        const profileResponse = await fetch(API_URL + "/worker/profile", {
            headers: {
                "Authorization": "Bearer " + workerToken
            }
        });

        const profile = await profileResponse.json();

        if (!profileResponse.ok) {
            throw new Error(profile.message || "Could not load worker profile.");
        }

        const workerId = profile.workerId;

        const response = await fetch(
            API_URL + "/reviews?workerId=" + workerId
        );

        const reviews = await response.json();

        if (!response.ok) {
            throw new Error("Could not load reviews.");
        }

        const summaryElement = document.getElementById("workerReviewSummary");

        if (!Array.isArray(reviews) || reviews.length === 0) {
            summaryElement.innerHTML = `
                <h3>No ratings yet</h3>
                <p>You have not received any customer reviews.</p>
            `;

            reviewsList.innerHTML = "<p>No customer reviews yet.</p>";
            return;
        }

        // Calculate average rating
        const totalRating = reviews.reduce(
            (total, review) => total + Number(review.rating), 0
        );

        const averageRating = totalRating / reviews.length;

        // Calculate star distribution
        const starCounts = [0, 0, 0, 0, 0];

        reviews.forEach(review => {
            const rating = Number(review.rating);

            if (rating >= 1 && rating <= 5) {
                starCounts[rating - 1]++;
            }
        });

        // Display summary
        summaryElement.innerHTML = `
            <div class="review-summary-card">
                <h2>${averageRating.toFixed(1)} / 5</h2>
                <p>${"★".repeat(Math.round(averageRating))}</p>
                <p>${reviews.length} Customer Review(s)</p>
            </div>

            <div class="star-distribution">
                ${[5, 4, 3, 2, 1].map(star => {
            const count = starCounts[star - 1];
            const percentage = (count / reviews.length) * 100;

            return `
                        <div class="star-row">
                            <span>${star} ★</span>
                            <div class="star-bar">
                                <div style="width: ${percentage}%"></div>
                            </div>
                            <span>${count}</span>
                        </div>
                    `;
        }).join("")}
            </div>
        `;

        // Display individual reviews
        reviewsList.innerHTML = reviews.map(review => `
            <div class="worker-review-card">
                <div class="review-card-header">
                    <h3>${escapeHTML(review.customer_name || "Customer")}</h3>
                    <span>${"★".repeat(Number(review.rating))}${"☆".repeat(5 - Number(review.rating))}</span>
                </div>

                <p>${escapeHTML(review.comment || "No comment provided.")}</p>

                <small>
                    ${escapeHTML(review.created_at || "")}
                </small>
            </div>
        `).join("");

    } catch (error) {
        console.error("Error loading worker reviews:", error);

        reviewsList.innerHTML =
            "<p class='error-message'>Could not load customer reviews: " +
            escapeHTML(error.message) + "</p>";
    }
}

async function loadWorkerRequests() {
    requestList.innerHTML = "<p>Loading requests...</p>";

    try {
        const response = await fetch(API_URL + "/worker/requests", {
            headers: {
                "Authorization": "Bearer " + workerToken
            }
        });

        const requests = await response.json();

        if (!response.ok) {
            throw new Error(requests.message || "Could not load requests.");
        }

        if (!Array.isArray(requests) || requests.length === 0) {
            requestList.innerHTML = "<p>No service requests found.</p>";
            return;
        }

        requestList.innerHTML = "";

        requests.forEach(request => {
            const card = document.createElement("div");
            card.className = "worker-request-card";

            const status = request.request_status || "UNKNOWN";
            const isPending = status === "PENDING";
            const isAccepted = status === "ACCEPTED";
            const isWorkerCompleted = status === "WORKER_COMPLETED";
            const isCompleted = status === "COMPLETED";

            const partsStatus = request.parts_status || "NOT_REQUIRED";

            const canComplete =
                isAccepted && partsStatus !== "PENDING";

            card.innerHTML = `
                <h3>${escapeHTML(request.service_name || "Service Request")}</h3>

                <p><strong>Request ID:</strong> #${request.request_id}</p>

                <p><strong>Customer:</strong>
                    ${escapeHTML(request.customer_name || "Customer")}
                </p>

                <p><strong>Problem:</strong>
                    ${escapeHTML(request.service_description || "Not provided")}
                </p>

                <p><strong>Service Price:</strong>
                    ₹${Number(request.service_price || 0).toFixed(2)}
                </p>

                <p><strong>Inspection Fee:</strong>
                    ₹${Number(request.inspection_fee || 0).toFixed(2)}
                </p>

                <p><strong>Appointment Date:</strong>
                    ${escapeHTML(request.appointment_date || "Not specified")}
                </p>

                <p><strong>Time:</strong>
                    ${escapeHTML(request.preferred_time_start || "")}
                    - ${escapeHTML(request.preferred_time_end || "")}
                </p>

                <p><strong>Status:</strong>
                    <span class="request-status">${escapeHTML(status.replaceAll("_", " "))}</span>
                </p>

                ${isPending ? `
                    <div class="request-actions">
                        <button class="accept-request">Accept</button>
                        <button class="reject-request">Reject</button>
                    </div>
                    <p class="request-message"></p>
                ` : ""}

                ${isAccepted || isWorkerCompleted || isCompleted ? `
                    <hr>
                    <h4>Additional Parts</h4>

                    <p><strong>Parts Status:</strong>
                        ${escapeHTML(partsStatus)}
                    </p>

                    ${request.parts_description ? `
                        <p><strong>Parts Description:</strong>
                            ${escapeHTML(request.parts_description)}
                        </p>
                        <p><strong>Parts Cost:</strong>
                            ₹${Number(request.parts_cost || 0).toFixed(2)}
                        </p>
                    ` : ""}
                ` : ""}

                ${isAccepted &&
                    (partsStatus === "NOT_REQUIRED" || partsStatus === "REJECTED") ? `
                    <div class="parts-proposal-form">
                        <label>Additional Parts Description</label>
                        <textarea class="parts-description"
                            maxlength="500"
                            placeholder="Describe the additional parts required"></textarea>

                        <label>Total Parts Cost (₹)</label>
                        <input class="parts-cost"
                            type="number"
                            min="0.01"
                            step="0.01"
                            placeholder="Enter total cost">

                        <button class="submit-parts-proposal">
                            Submit Parts Proposal
                        </button>
                    </div>
                ` : ""}

                ${canComplete ? `
                    <div class="completion-section">
                        <button class="complete-request">
                            Mark as Completed
                        </button>
                        <p class="completion-message"></p>
                    </div>
                ` : ""}

                ${isAccepted && partsStatus === "PENDING" ? `
                    <p class="request-message">
                        Waiting for customer approval of additional parts.
                    </p>
                ` : ""}

                ${isWorkerCompleted ? `
                    <p class="request-message">
                        Waiting for customer confirmation.
                    </p>
                ` : ""}

                ${isCompleted ? `
                    <p class="request-message">
                        Customer confirmed this service as completed.
                    </p>
                ` : ""}

                <p class="parts-message"></p>
            `;

            if (isPending) {
                card.querySelector(".accept-request").addEventListener("click", () => {
                    updateRequest(request.request_id, "ACCEPTED", card);
                });

                card.querySelector(".reject-request").addEventListener("click", () => {
                    updateRequest(request.request_id, "REJECTED", card);
                });
            }

            if (isAccepted &&
                (partsStatus === "NOT_REQUIRED" || partsStatus === "REJECTED")) {

                card.querySelector(".submit-parts-proposal")
                    .addEventListener("click", () => {
                        submitPartsProposal(request.request_id, card);
                    });
            }

            if (canComplete) {
                card.querySelector(".complete-request")
                    .addEventListener("click", () => {
                        completeRequest(request.request_id, card);
                    });
            }

            requestList.appendChild(card);
        });

    } catch (error) {
        requestList.innerHTML =
            "<p>Could not load requests: " + escapeHTML(error.message) + "</p>";
    }
}

async function updateRequest(requestId, decision, card) {
    const message = card.querySelector(".request-message");
    message.textContent = "Updating request...";

    try {
        const response = await fetch(API_URL + "/worker/requests", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": "Bearer " + workerToken
            },
            body: JSON.stringify({
                requestId: requestId,
                decision: decision
            })
        });

        const result = await response.json();

        if (!response.ok) {
            throw new Error(result.message || "Request update failed.");
        }

        message.textContent = result.message || "Request updated successfully.";
        await loadWorkerRequests();

    } catch (error) {
        message.textContent = error.message;
    }
}

async function submitPartsProposal(requestId, card) {
    const description = card.querySelector(".parts-description").value.trim();
    const cost = Number(card.querySelector(".parts-cost").value);
    const message = card.querySelector(".parts-message");

    if (!description || description.length > 500) {
        message.textContent = "Enter a description of 1-500 characters.";
        return;
    }

    if (!Number.isFinite(cost) || cost <= 0) {
        message.textContent = "Enter a valid cost greater than zero.";
        return;
    }

    message.textContent = "Submitting proposal...";

    try {
        const response = await fetch(API_URL + "/worker/parts-proposal", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": "Bearer " + workerToken
            },
            body: JSON.stringify({
                requestId: requestId,
                description: description,
                cost: cost
            })
        });

        const result = await response.json();

        if (!response.ok) {
            throw new Error(result.message || "Could not submit parts proposal.");
        }

        message.textContent = result.message;
        await loadWorkerRequests();

    } catch (error) {
        message.textContent = error.message;
    }
}

async function completeRequest(requestId, card) {
    const message = card.querySelector(".completion-message");

    if (!confirm("Have you finished this service?")) {
        return;
    }

    message.textContent = "Updating service status...";

    try {
        const response = await fetch(API_URL + "/worker/complete-request", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": "Bearer " + workerToken
            },
            body: JSON.stringify({
                requestId: requestId
            })
        });

        const result = await response.json();

        if (!response.ok) {
            throw new Error(result.message || "Could not complete the service.");
        }

        alert(result.message);
        await loadWorkerRequests();

    } catch (error) {
        message.textContent = error.message;
    }
}


// Load pending revisit requests assigned to the logged-in worker
async function loadWorkerRevisitRequests() {
    const revisitList = document.getElementById("workerRevisitList");

    if (!revisitList) return;

    revisitList.innerHTML = "<p>Loading revisit requests...</p>";

    try {
        const response = await fetch(
            API_URL + "/worker/revisit-requests",
            {
                headers: {
                    "Authorization": "Bearer " + workerToken
                }
            }
        );

        const revisits = await response.json();

        if (!response.ok) {
            throw new Error(
                revisits.message || "Could not load revisit requests."
            );
        }

        if (!Array.isArray(revisits) || revisits.length === 0) {
            revisitList.innerHTML =
                "<p>No pending revisit requests.</p>";
            return;
        }

        revisitList.innerHTML = "";

        revisits.forEach(revisit => {
            const card = document.createElement("div");
            card.className = "worker-request-card";

            // Tomorrow is the earliest valid revisit date.
            const tomorrow = new Date();
            tomorrow.setDate(tomorrow.getDate() + 1);

            const minDate =
                tomorrow.getFullYear() + "-" +
                String(tomorrow.getMonth() + 1).padStart(2, "0") + "-" +
                String(tomorrow.getDate()).padStart(2, "0");

            card.innerHTML = `
                <h3>${escapeHTML(revisit.service_name || "Service")}</h3>

                <p><strong>Request ID:</strong> #${revisit.request_id}</p>

                <p><strong>Customer:</strong>
                    ${escapeHTML(revisit.customer_name || "Customer")}
                </p>

                <p><strong>Problem:</strong>
                    ${escapeHTML(revisit.service_description || "Not provided")}
                </p>

                <hr>

                <h4>Propose a New Appointment</h4>

                <label>Revisit Date</label>
                <input type="date" class="revisit-date" min="${minDate}">

                <label>Start Time</label>
                <input type="time" class="revisit-start-time">

                <label>End Time</label>
                <input type="time" class="revisit-end-time">

                <button class="submit-revisit-proposal">
                    Propose Appointment
                </button>

                <p class="revisit-message"></p>
            `;

            card.querySelector(".submit-revisit-proposal")
                .addEventListener("click", () => {
                    submitRevisitProposal(revisit.request_id, card);
                });

            revisitList.appendChild(card);
        });

    } catch (error) {
        console.error("Error loading revisit requests:", error);

        revisitList.innerHTML =
            "<p class='error-message'>Could not load revisit requests: " +
            escapeHTML(error.message) + "</p>";
    }
}


// Submit a proposed revisit appointment
async function submitRevisitProposal(requestId, card) {
    const date = card.querySelector(".revisit-date").value;
    const startTime = card.querySelector(".revisit-start-time").value;
    const endTime = card.querySelector(".revisit-end-time").value;
    const message = card.querySelector(".revisit-message");

    if (!date || !startTime || !endTime) {
        message.textContent = "Please enter the date and both times.";
        return;
    }

    if (endTime <= startTime) {
        message.textContent = "End time must be later than start time.";
        return;
    }

    if (!confirm("Submit this revisit appointment proposal?")) {
        return;
    }

    message.textContent = "Submitting appointment proposal...";

    try {
        const response = await fetch(
            API_URL + "/worker/revisit-proposal",
            {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": "Bearer " + workerToken
                },
                body: JSON.stringify({
                    requestId: requestId,
                    date: date,
                    startTime: startTime,
                    endTime: endTime
                })
            }
        );

        const result = await response.json();

        if (!response.ok) {
            throw new Error(
                result.message || "Could not submit appointment proposal."
            );
        }

        alert(result.message || "Appointment proposal submitted!");

        await loadWorkerRevisitRequests();

    } catch (error) {
        message.textContent = error.message;
    }
}

function escapeHTML(value) {
    return String(value).replace(/[&<>"']/g, character => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;"
    })[character]);
}

const refreshRequestsButton = document.getElementById("refreshRequests");

if (refreshRequestsButton) {
    refreshRequestsButton.addEventListener("click", loadWorkerRequests);
}

if (workerToken && workerRole === "WORKER") {
    loadWorkerRequests();
    loadWorkerReviews();
    loadWorkerRevisitRequests();
}

// =====================================
// WORKER AVAILABILITY MANAGEMENT
// =====================================

const availabilityForm = document.getElementById("availabilityForm");
const availabilityList = document.getElementById("workerAvailabilityList");
const availabilityMessage = document.getElementById("availabilityMessage");
const refreshAvailabilityButton = document.getElementById("refreshAvailability");
function formatTime(time) {
    const [hours, minutes] = time.slice(0, 5).split(":");

    const date = new Date();
    date.setHours(Number(hours), Number(minutes));

    return date.toLocaleTimeString("en-US", {
        hour: "numeric",
        minute: "2-digit",
        hour12: true
    });
}

// Load availability slots from the backend
async function loadWorkerAvailability() {
    if (!availabilityList) return;

    availabilityList.innerHTML = "<p>Loading availability...</p>";

    try {
        const response = await fetch(API_URL + "/worker/availability", {
            headers: {
                "Authorization": "Bearer " + workerToken
            }
        });

        const slots = await response.json();

        if (!response.ok) {
            throw new Error(slots.message || "Could not load availability.");
        }

        if (!Array.isArray(slots) || slots.length === 0) {
            availabilityList.innerHTML =
                "<p>You have not added any availability slots.</p>";
            return;
        }

        availabilityList.innerHTML = slots.map(slot => `
            <div class="availability-slot-card"
                 data-id="${Number(slot.availabilityId)}">

                <h4>${escapeHTML(slot.day)}</h4>

                <p>
                   ${formatTime(slot.startTime)}
                   - ${formatTime(slot.endTime)}
                </p>

               <p class="availability-status ${slot.isAvailable ? "available" : "unavailable"}">
                 ${slot.isAvailable ? "● Available" : "● Unavailable"}
                </p>

                <button type="button" class="edit-availability">
                    Edit
                </button>

                <button type="button" class="delete-availability">
                    Delete
                </button>

            </div>
        `).join("");

        availabilityList.querySelectorAll(".edit-availability")
            .forEach(button => {
                button.addEventListener("click", () => {
                    editAvailability(button.closest(".availability-slot-card"), slots);
                });
            });

        availabilityList.querySelectorAll(".delete-availability")
            .forEach(button => {
                button.addEventListener("click", () => {
                    deleteAvailability(button.closest(".availability-slot-card"));
                });
            });

    } catch (error) {
        console.error("Availability loading error:", error);

        availabilityList.innerHTML =
            "<p class='error-message'>" +
            escapeHTML(error.message) + "</p>";
    }
}


// Add or update an availability slot
availabilityForm?.addEventListener("submit", async function (event) {
    event.preventDefault();

    const day = document.getElementById("availableDay").value;
    const startTime = document.getElementById("availableStartTime").value;
    const endTime = document.getElementById("availableEndTime").value;

    const editingId = availabilityForm.dataset.editingId;
    const isEditing = Boolean(editingId);

    if (!day || !startTime || !endTime) {
        availabilityMessage.textContent = "Please fill in all fields.";
        return;
    }

    if (endTime <= startTime) {
        availabilityMessage.textContent =
            "End time must be later than start time.";
        return;
    }

    availabilityMessage.textContent =
        isEditing ? "Updating availability..." : "Adding availability...";

    try {
        const response = await fetch(API_URL + "/worker/availability", {
            method: isEditing ? "PUT" : "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": "Bearer " + workerToken
            },
            body: JSON.stringify({
                availabilityId: isEditing ? Number(editingId) : undefined,
                day: day,
                startTime: startTime,
                endTime: endTime
            })
        });

        const result = await response.json();

        if (!response.ok) {
            throw new Error(
                result.message || "Availability operation failed."
            );
        }

        availabilityMessage.textContent = result.message;

        delete availabilityForm.dataset.editingId;

        availabilityForm.reset();

        availabilityForm.querySelector(
            'button[type="submit"]'
        ).textContent = "Add Availability";

        await loadWorkerAvailability();

    } catch (error) {
        availabilityMessage.textContent = error.message;
    }
});

// Edit an existing availability slot
function editAvailability(card, slots) {
    const availabilityId = Number(card.dataset.id);

    const slot = slots.find(item =>
        Number(item.availabilityId) === availabilityId
    );

    if (!slot) return;

    document.getElementById("availableDay").value = slot.day;

    document.getElementById("availableStartTime").value =
        slot.startTime.slice(0, 5);

    document.getElementById("availableEndTime").value =
        slot.endTime.slice(0, 5);

    availabilityForm.dataset.editingId = availabilityId;

    const submitButton = availabilityForm.querySelector(
        'button[type="submit"]'
    );

    submitButton.textContent = "Update Availability";

    availabilityMessage.textContent =
        "Edit the details above, then click Update Availability.";

    availabilityForm.scrollIntoView({
        behavior: "smooth",
        block: "center"
    });
}


// Delete an availability slot
async function deleteAvailability(card) {
    const availabilityId = Number(card.dataset.id);

    if (!confirm("Are you sure you want to delete this availability slot?")) {
        return;
    }

    try {
        const response = await fetch(API_URL + "/worker/availability", {
            method: "DELETE",
            headers: {
                "Content-Type": "application/json",
                "Authorization": "Bearer " + workerToken
            },
            body: JSON.stringify({
                availabilityId: availabilityId
            })
        });

        const result = await response.json();

        if (!response.ok) {
            throw new Error(result.message || "Could not delete availability.");
        }

        availabilityMessage.textContent = result.message;

        await loadWorkerAvailability();

    } catch (error) {
        availabilityMessage.textContent = error.message;
    }
}


// Refresh availability list
refreshAvailabilityButton?.addEventListener(
    "click",
    loadWorkerAvailability
);


// Load saved availability when dashboard opens
if (workerToken && workerRole === "WORKER") {
    loadWorkerAvailability();
}

// =====================================
// WORKER SERVICE PRICE MANAGEMENT
// =====================================

const workerPriceList = document.getElementById("workerPriceList");
const refreshPricesButton = document.getElementById("refreshPrices");
const priceMessage = document.getElementById("priceMessage");

const servicePriceForm = document.getElementById("servicePriceForm");
const serviceNameInput = document.getElementById("serviceName");
const servicePriceInput = document.getElementById("servicePrice");
const inspectionFeeInput = document.getElementById("inspectionFee");

const savePriceButton = document.getElementById("savePriceButton");
const cancelPriceEditButton = document.getElementById("cancelPriceEdit");
const priceFormTitle = document.getElementById("priceFormTitle");

let editingPriceId = null;


// Load prices from backend
async function loadWorkerPrices() {
    if (!workerPriceList) return;

    workerPriceList.innerHTML = "<p>Loading your service prices...</p>";

    try {
        const response = await fetch(API_URL + "/worker/prices", {
            headers: {
                "Authorization": "Bearer " + workerToken
            }
        });

        const prices = await response.json();

        if (!response.ok) {
            throw new Error(
                prices.message || "Could not load service prices."
            );
        }

        if (!Array.isArray(prices) || prices.length === 0) {
            workerPriceList.innerHTML =
                "<p>You have not added any service prices yet.</p>";
            return;
        }

        workerPriceList.innerHTML = prices.map(price => `
    <div class="availability-slot-card"
         data-id="${Number(price.priceId)}">

        <h4>${escapeHTML(price.serviceName)}</h4>

        <p>
            <strong>Service Price:</strong>
            ₹${Number(price.servicePrice).toFixed(2)}
        </p>

        <p>
            <strong>Inspection Fee:</strong>
            ₹${Number(price.inspectionFee).toFixed(2)}
        </p>

        <button type="button" class="edit-price">
            Edit Price
        </button>

        <button type="button" class="delete-price">
            Delete
        </button>

    </div>
`).join("");

        workerPriceList.querySelectorAll(".edit-price")
            .forEach((button, index) => {
                button.addEventListener("click", () => {
                    editServicePrice(prices[index]);
                });
            });
        workerPriceList.querySelectorAll(".delete-price")
            .forEach((button, index) => {
                button.addEventListener("click", () => {
                    deleteServicePrice(prices[index].priceId);
                });
            });
    } catch (error) {
        console.error("Price loading error:", error);

        workerPriceList.innerHTML =
            "<p class='error-message'>" +
            escapeHTML(error.message) + "</p>";
    }

}

//delete a service price
async function deleteServicePrice(priceId) {
    if (!confirm("Are you sure you want to delete this service price?")) {
        return;
    }

    priceMessage.textContent = "Deleting service price...";

    try {
        const response = await fetch(API_URL + "/worker/prices/delete", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": "Bearer " + workerToken
            },
            body: JSON.stringify({
                priceId: Number(priceId)
            })
        });

        const result = await response.json();

        if (!response.ok) {
            throw new Error(result.message || "Could not delete service price.");
        }

        priceMessage.textContent =
            result.message || "Service price deleted successfully.";

        await loadWorkerPrices();

    } catch (error) {
        priceMessage.textContent = error.message;
    }
}


// Add or update a service price
servicePriceForm?.addEventListener("submit", async function (event) {
    event.preventDefault();

    const serviceName = serviceNameInput.value.trim();
    const servicePrice = Number(servicePriceInput.value);
    const inspectionFee = Number(inspectionFeeInput.value);

    if (!serviceName) {
        priceMessage.textContent = "Enter a service name.";
        return;
    }

    if (!Number.isFinite(servicePrice) || servicePrice <= 0) {
        priceMessage.textContent =
            "Service price must be greater than zero.";
        return;
    }

    if (!Number.isFinite(inspectionFee) || inspectionFee < 0) {
        priceMessage.textContent =
            "Inspection fee cannot be negative.";
        return;
    }

    priceMessage.textContent =
        editingPriceId === null
            ? "Saving service price..."
            : "Updating service price...";

    savePriceButton.disabled = true;

    try {
        const response = await fetch(API_URL + "/worker/prices", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": "Bearer " + workerToken
            },
            body: JSON.stringify({
                priceId: editingPriceId,
                serviceName: serviceName,
                servicePrice: servicePrice,
                inspectionFee: inspectionFee
            })
        });

        const result = await response.json();

        if (!response.ok) {
            throw new Error(
                result.message || "Could not save service price."
            );
        }

        priceMessage.textContent =
            result.message || "Service price saved successfully.";

        resetPriceForm();

        await loadWorkerPrices();

    } catch (error) {
        priceMessage.textContent = error.message;

    } finally {
        savePriceButton.disabled = false;
    }
});


// Load existing price into the form for editing
function editServicePrice(price) {
    editingPriceId = Number(price.priceId);

    serviceNameInput.value = price.serviceName;
    servicePriceInput.value = price.servicePrice;
    inspectionFeeInput.value = price.inspectionFee;

    priceFormTitle.textContent = "Edit Service Price";
    savePriceButton.textContent = "Update Service Price";
    cancelPriceEditButton.hidden = false;

    priceMessage.textContent =
        "Update the details and click Update Service Price.";

    servicePriceForm.scrollIntoView({
        behavior: "smooth",
        block: "center"
    });
}


// Reset form after saving or cancelling
function resetPriceForm() {
    servicePriceForm.reset();

    editingPriceId = null;

    priceFormTitle.textContent = "Add a Service Price";
    savePriceButton.textContent = "Add Service Price";
    cancelPriceEditButton.hidden = true;
}


// Cancel editing
cancelPriceEditButton?.addEventListener("click", () => {
    resetPriceForm();
    priceMessage.textContent = "Editing cancelled.";
});


// Refresh prices
refreshPricesButton?.addEventListener("click", loadWorkerPrices);


// Load prices when dashboard opens
if (workerToken && workerRole === "WORKER") {
    loadWorkerPrices();
}

const workerNavButtons = document.querySelectorAll(".worker-nav-link");
const workerPages = document.querySelectorAll(".worker-page");

workerNavButtons.forEach(button => {
    button.addEventListener("click", function () {
        const selectedPage = this.dataset.page;

        workerPages.forEach(page => {
            page.classList.remove("active");
        });

        workerNavButtons.forEach(navButton => {
            navButton.classList.remove("active");
        });

        const pageToShow = document.getElementById(selectedPage);

        if (pageToShow) {
            pageToShow.classList.add("active");
            this.classList.add("active");
        }
    });
});


// Worker logout
const workerLogoutButton = document.getElementById("workerLogoutButton");

workerLogoutButton?.addEventListener("click", async function () {

    if (!confirm("Are you sure you want to log out?")) {
        return;
    }

    workerLogoutButton.disabled = true;
    workerLogoutButton.textContent = "Logging out...";

    try {
        await fetch(API_URL + "/logout", {
            method: "POST",
            headers: {
                "Authorization": "Bearer " + workerToken
            }
        });

    } catch (error) {
        console.error("Logout request failed:", error);

    } finally {
        localStorage.removeItem("serveUToken");
        localStorage.removeItem("serveURole");
        localStorage.removeItem("serveUName");
        localStorage.removeItem("serveUUserId");

        window.location.href = "login.html";
    }
});
