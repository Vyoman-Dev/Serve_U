import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;

public class Requests {

    // WORKER: Accept or reject a service request
    public static void updateRequestStatus(
            Scanner scanner, int workerId) {

        System.out.print("Enter Request ID: ");

        int requestId;

        try {
            requestId = Integer.parseInt(scanner.nextLine());

            if (requestId <= 0) {
                System.out.println("Invalid Request ID.");
                return;
            }

        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid Request ID.");
            return;
        }

        System.out.println("1. Accept Request");
        System.out.println("2. Reject Request");
        System.out.print("Enter your decision: ");

        String choice = scanner.nextLine();
        String newStatus;

        if (choice.equals("1")) {
            newStatus = "ACCEPTED";
        } else if (choice.equals("2")) {
            newStatus = "REJECTED";
        } else {
            System.out.println("Invalid choice.");
            return;
        }

        String sql = "UPDATE service_requests "
                + "SET request_status = ? "
                + "WHERE request_id = ? "
                + "AND worker_id = ? "
                + "AND request_status = 'PENDING'";

        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, newStatus);
            ps.setInt(2, requestId);
            ps.setInt(3, workerId);

            int rowsUpdated = ps.executeUpdate();

            if (rowsUpdated > 0) {
                System.out.println(
                        "Request successfully " + newStatus + "!");
            } else {
                System.out.println(
                        "Request not found, not assigned to you, "
                                + "or already processed.");
            }

        } catch (SQLException e) {
            System.out.println("Could not update request status.");
            e.printStackTrace();
        }
    }

    // WORKER: Report that a service is finished
    public static void markRequestCompleted(
            Scanner scanner, int workerId) {

        System.out.print("Enter Request ID: ");

        int requestId;

        try {
            requestId = Integer.parseInt(scanner.nextLine());

            if (requestId <= 0) {
                System.out.println("Invalid Request ID.");
                return;
            }

        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid Request ID.");
            return;
        }

        String sql = "UPDATE service_requests "
                + "SET request_status = 'WORKER_COMPLETED' "
                + "WHERE request_id = ? "
                + "AND worker_id = ? "
                + "AND request_status IN ('ACCEPTED', 'REVISIT_SCHEDULED') "
                + "AND (parts_status IS NULL OR parts_status <> 'PENDING')";

        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, requestId);
            ps.setInt(2, workerId);

            int rowsUpdated = ps.executeUpdate();

            if (rowsUpdated > 0) {
                System.out.println(
                        "Service completion reported successfully!");
                System.out.println(
                        "Waiting for customer confirmation.");
            } else {
                System.out.println(
                        "Request cannot be completed. Check that it belongs to you, "
                                + "is ACCEPTED or REVISIT_SCHEDULED, and has no pending parts proposal.");
            }

        } catch (SQLException e) {
            System.out.println("Could not update service status.");
            e.printStackTrace();
        }
    }

    // WORKER: Propose additional parts and their cost
    public static void proposeAdditionalParts(
            Scanner scanner, int workerId) {

        System.out.print("Enter Request ID: ");
        int requestId;

        try {
            requestId = Integer.parseInt(scanner.nextLine());

            if (requestId <= 0) {
                System.out.println("Invalid Request ID.");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("Enter a valid Request ID.");
            return;
        }

        System.out.print("Describe the additional parts: ");
        String description = scanner.nextLine().trim();

        if (description.isEmpty() || description.length() > 500) {
            System.out.println("Description must contain 1-500 characters.");
            return;
        }

        System.out.print("Enter total parts cost: Rs. ");
        java.math.BigDecimal cost;

        try {
            cost = new java.math.BigDecimal(scanner.nextLine());

            if (cost.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                System.out.println("Cost must be greater than zero.");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("Enter a valid cost.");
            return;
        }

        String sql = "UPDATE service_requests "
                + "SET parts_description = ?, "
                + "parts_cost = ?, "
                + "parts_status = 'PENDING' "
                + "WHERE request_id = ? "
                + "AND worker_id = ? "
                + "AND request_status = 'ACCEPTED' "
                + "AND parts_status IN ('NOT_REQUIRED', 'REJECTED')";

        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, description);
            ps.setBigDecimal(2, cost);
            ps.setInt(3, requestId);
            ps.setInt(4, workerId);

            int rowsUpdated = ps.executeUpdate();

            if (rowsUpdated > 0) {
                System.out.println("Parts proposal submitted successfully!");
                System.out.println("Additional cost: Rs. " + cost);
                System.out.println("Waiting for customer approval.");
            } else {
                System.out.println(
                        "Request not found, not assigned to you, "
                                + "or not eligible for a parts proposal.");
            }

        } catch (SQLException e) {
            System.out.println("Could not submit parts proposal.");
            e.printStackTrace();
        }
    }

    // CUSTOMER: Approve or reject additional parts
    public static void respondToPartsProposal(
            Scanner scanner, int customerId) {

        String viewSql = "SELECT request_id, service_name, "
                + "parts_description, parts_cost "
                + "FROM service_requests "
                + "WHERE customer_id = ? "
                + "AND parts_status = 'PENDING'";

        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(viewSql)) {

            ps.setInt(1, customerId);

            try (ResultSet rs = ps.executeQuery()) {

                boolean found = false;

                while (rs.next()) {
                    found = true;

                    System.out.println("\nRequest ID: "
                            + rs.getInt("request_id"));

                    System.out.println("Service: "
                            + rs.getString("service_name"));

                    System.out.println("Additional Parts: "
                            + rs.getString("parts_description"));

                    System.out.println("Additional Cost: Rs. "
                            + rs.getBigDecimal("parts_cost"));
                }

                if (!found) {
                    System.out.println("No pending parts proposals.");
                    return;
                }
            }

        } catch (SQLException e) {
            System.out.println("Could not retrieve parts proposals.");
            e.printStackTrace();
            return;
        }

        System.out.print("\nEnter Request ID: ");
        int requestId;

        try {
            requestId = Integer.parseInt(scanner.nextLine());

            if (requestId <= 0) {
                System.out.println("Invalid Request ID.");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("Enter a valid Request ID.");
            return;
        }

        System.out.println("1. Approve additional parts");
        System.out.println("2. Reject additional parts");
        System.out.print("Enter choice: ");

        String choice = scanner.nextLine();

        String status;

        if (choice.equals("1")) {
            status = "APPROVED";
        } else if (choice.equals("2")) {
            status = "REJECTED";
        } else {
            System.out.println("Invalid choice.");
            return;
        }

        String updateSql = "UPDATE service_requests "
                + "SET parts_status = ? "
                + "WHERE request_id = ? "
                + "AND customer_id = ? "
                + "AND parts_status = 'PENDING' "
                + "AND request_status = 'ACCEPTED'";

        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(updateSql)) {

            ps.setString(1, status);
            ps.setInt(2, requestId);
            ps.setInt(3, customerId);

            int rowsUpdated = ps.executeUpdate();

            if (rowsUpdated > 0) {
                System.out.println("Parts proposal " + status + " successfully!");
            } else {
                System.out.println(
                        "Proposal not found, not yours, or no longer pending.");
            }

        } catch (SQLException e) {
            System.out.println("Could not update parts decision.");
            e.printStackTrace();
        }
    }

    // CUSTOMER: Create a service request
    public static boolean createRequest(
            Scanner scanner, int customerId, int workerId) {

        try (Connection con = DatabaseConnection.getConnection()) {

            // Step 1: Verify worker
            String checkWorkerSql = "SELECT w.worker_id FROM workers w " +
                    "JOIN users u ON w.user_id = u.user_id " +
                    "WHERE w.worker_id = ? " +
                    "AND w.verification_status = 'APPROVED' " +
                    "AND u.account_status = 'ACTIVE'";

            try (PreparedStatement check = con.prepareStatement(checkWorkerSql)) {
                check.setInt(1, workerId);

                try (ResultSet rs = check.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("Invalid worker ID or worker is not approved.");
                        return false;
                    }
                }
            }

            // Step 2: Display worker's services
            String priceSql = "SELECT price_id, service_name, service_price, inspection_fee " +
                    "FROM worker_service_prices WHERE worker_id = ?";

            int selectedPriceId = -1;

            try (PreparedStatement priceStmt = con.prepareStatement(priceSql)) {

                priceStmt.setInt(1, workerId);

                try (ResultSet rs = priceStmt.executeQuery()) {

                    boolean found = false;

                    System.out.println("\nAvailable Services:");

                    while (rs.next()) {
                        found = true;

                        System.out.println(
                                rs.getInt("price_id") + ". " +
                                        rs.getString("service_name") +
                                        " | Price: Rs. " +
                                        rs.getBigDecimal("service_price") +
                                        " | Inspection: Rs. " +
                                        rs.getBigDecimal("inspection_fee"));
                    }

                    if (!found) {
                        System.out.println("This worker has not added any services yet.");
                        return false;
                    }
                }
            }

            // Step 3: Select service
            System.out.print("\nEnter the service ID you want: ");

            try {
                selectedPriceId = Integer.parseInt(scanner.nextLine());

                if (selectedPriceId <= 0) {
                    System.out.println("Invalid service ID.");
                    return false;
                }

            } catch (NumberFormatException e) {
                System.out.println("Enter a valid service ID.");
                return false;
            }

            // Step 4: Retrieve selected service and prices
            String selectedSql = "SELECT service_name, service_price, inspection_fee " +
                    "FROM worker_service_prices " +
                    "WHERE price_id = ? AND worker_id = ?";

            String serviceName;
            java.math.BigDecimal servicePrice;
            java.math.BigDecimal inspectionFee;

            try (PreparedStatement selectedStmt = con.prepareStatement(selectedSql)) {

                selectedStmt.setInt(1, selectedPriceId);
                selectedStmt.setInt(2, workerId);

                try (ResultSet rs = selectedStmt.executeQuery()) {

                    if (!rs.next()) {
                        System.out.println("Invalid service selection.");
                        return false;
                    }

                    serviceName = rs.getString("service_name");
                    servicePrice = rs.getBigDecimal("service_price");
                    inspectionFee = rs.getBigDecimal("inspection_fee");
                }
            }

            // Step 5: Enter work description
            System.out.print("Describe the work you need: ");
            String serviceDescription = scanner.nextLine().trim();

            if (serviceDescription.isEmpty()) {
                System.out.println("Service description cannot be empty.");
                return false;

            }
            // Step 6: Enter appointment details
            System.out.print("Enter appointment date (YYYY-MM-DD, DD/MM/YYYY or DD-MM-YYYY): ");
            String appointmentDate = scanner.nextLine().trim();

            System.out.print("Enter preferred start time (HH:MM): ");
            String startTime = scanner.nextLine().trim();

            System.out.print("Enter preferred end time (HH:MM): ");
            String endTime = scanner.nextLine().trim();

            LocalDate date;
            LocalTime start;
            LocalTime end;

            try {
                DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                        .appendPattern("[yyyy-MM-dd][dd/MM/yyyy][dd-MM-yyyy]")
                        .toFormatter();

                date = LocalDate.parse(appointmentDate, formatter);
                start = LocalTime.parse(startTime);
                end = LocalTime.parse(endTime);

                if (!date.isAfter(LocalDate.now())) {
                    System.out.println("Appointment date must be in the future.");
                    return false;
                }

                if (!end.isAfter(start)) {
                    System.out.println("End time must be later than start time.");
                    return false;
                }

            } catch (DateTimeParseException e) {
                System.out.println("Invalid date or time format.");
                return false;
            }
            // Step 6.5: Check worker availability

            String availabilitySql = "SELECT availability_id FROM worker_availability " +
                    "WHERE worker_id = ? " +
                    "AND available_day = ? " +
                    "AND is_available = 1 " +
                    "AND start_time <= ? " +
                    "AND end_time >= ?";

            try (PreparedStatement availabilityStmt = con.prepareStatement(availabilitySql)) {

                availabilityStmt.setInt(1, workerId);

                // Convert appointment date into MONDAY, TUESDAY, etc.
                String day = date.getDayOfWeek().name();

                availabilityStmt.setString(2, day);
                availabilityStmt.setTime(3, java.sql.Time.valueOf(start));
                availabilityStmt.setTime(4, java.sql.Time.valueOf(end));

                try (ResultSet availabilityRs = availabilityStmt.executeQuery()) {

                    if (!availabilityRs.next()) {
                        System.out.println(
                                "Worker is not available on this day or during this time.");
                        return false;
                    }
                }
            }
            // Step 6.6: Prevent overlapping bookings

            String overlapSql = "SELECT request_id FROM service_requests " +
                    "WHERE worker_id = ? " +
                    "AND appointment_date = ? " +
                    "AND request_status IN ('PENDING', 'ACCEPTED', 'DISPUTED') " +
                    "AND preferred_time_start < ? " +
                    "AND preferred_time_end > ?";

            try (PreparedStatement overlapStmt = con.prepareStatement(overlapSql)) {

                overlapStmt.setInt(1, workerId);
                overlapStmt.setDate(2, java.sql.Date.valueOf(date));
                overlapStmt.setTime(3, java.sql.Time.valueOf(end));
                overlapStmt.setTime(4, java.sql.Time.valueOf(start));

                try (ResultSet overlapRs = overlapStmt.executeQuery()) {

                    if (overlapRs.next()) {
                        System.out.println(
                                "This worker already has a booking during that time.");
                        return false;
                    }
                }
            }

            // Step 7: Save request with price snapshot
            String insertSql = "INSERT INTO service_requests " +
                    "(customer_id, worker_id, service_name, service_description, " +
                    "service_price, inspection_fee, appointment_date, " +
                    "preferred_time_start, preferred_time_end) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement ps = con.prepareStatement(insertSql)) {

                ps.setInt(1, customerId);
                ps.setInt(2, workerId);
                ps.setString(3, serviceName);
                ps.setString(4, serviceDescription);
                ps.setBigDecimal(5, servicePrice);
                ps.setBigDecimal(6, inspectionFee);
                ps.setDate(7, java.sql.Date.valueOf(date));
                ps.setTime(8, java.sql.Time.valueOf(start));
                ps.setTime(9, java.sql.Time.valueOf(end));

                int rowsInserted = ps.executeUpdate();

                if (rowsInserted > 0) {
                    System.out.println("\nService request created successfully!");
                    System.out.println("Selected Service: " + serviceName);
                    System.out.println("Service Price: Rs. " + servicePrice);
                    System.out.println("Inspection Fee: Rs. " + inspectionFee);
                    System.out.println("Appointment Date: " + date);
                    System.out.println("Preferred Time: " + start + " to " + end);
                    return true;
                }
            }

        } catch (SQLException e) {
            System.out.println("Could not create service request.");
            e.printStackTrace();
        }

        return false;
    }

    // WORKER: View incoming service requests
    public static void viewWorkerRequests(int workerId) {

        String sql = "SELECT request_id, customer_id, service_name, "
                + "service_description, service_price, inspection_fee, "
                + "parts_cost, appointment_date, preferred_time_start, "
                + "preferred_time_end, request_status, created_at "
                + "FROM service_requests "
                + "WHERE worker_id = ? "
                + "ORDER BY created_at DESC";
        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, workerId);

            try (ResultSet rs = ps.executeQuery()) {

                boolean found = false;

                while (rs.next()) {
                    found = true;

                    System.out.println("\nRequest ID: "
                            + rs.getInt("request_id"));

                    System.out.println("Customer ID: "
                            + rs.getInt("customer_id"));

                    System.out.println("Service: "
                            + rs.getString("service_name"));

                    System.out.println("Description: "
                            + rs.getString("service_description"));

                    System.out.println("Service Price: Rs. "
                            + rs.getBigDecimal("service_price"));

                    System.out.println("Inspection Fee: Rs. "
                            + rs.getBigDecimal("inspection_fee"));

                    System.out.println("Parts Cost: Rs. "
                            + rs.getBigDecimal("parts_cost"));

                    System.out.println("Appointment Date: "
                            + rs.getDate("appointment_date"));

                    System.out.println("Preferred Time: "
                            + rs.getTime("preferred_time_start")
                            + " to "
                            + rs.getTime("preferred_time_end"));
                    System.out.println("Status: "
                            + rs.getString("request_status"));

                    System.out.println("Created At: "
                            + rs.getTimestamp("created_at"));

                    System.out.println("-------------------------");
                }

                if (!found) {
                    System.out.println("No incoming service requests.");
                }
            }

        } catch (SQLException e) {
            System.out.println("Could not retrieve service requests.");
            e.printStackTrace();
        }
    }

    // CUSTOMER: View their own service requests
    public static void viewCustomerRequests(int customerId) {

        String sql = "SELECT request_id, worker_id, service_name, "
                + "service_price, inspection_fee, parts_cost, "
                + "appointment_date, preferred_time_start, "
                + "preferred_time_end, request_status "
                + "FROM service_requests "
                + "WHERE customer_id = ? "
                + "ORDER BY created_at DESC";

        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, customerId);

            try (ResultSet rs = ps.executeQuery()) {

                boolean found = false;

                while (rs.next()) {
                    found = true;

                    System.out.println("\nRequest ID: "
                            + rs.getInt("request_id"));

                    System.out.println("Worker ID: "
                            + rs.getInt("worker_id"));

                    System.out.println("Service: "
                            + rs.getString("service_name"));

                    System.out.println("Service Price: Rs. "
                            + rs.getBigDecimal("service_price"));

                    System.out.println("Inspection Fee: Rs. "
                            + rs.getBigDecimal("inspection_fee"));

                    System.out.println("Parts Cost: Rs. "
                            + rs.getBigDecimal("parts_cost"));

                    System.out.println("Appointment Date: "
                            + rs.getDate("appointment_date"));

                    System.out.println("Preferred Time: "
                            + rs.getTime("preferred_time_start")
                            + " to "
                            + rs.getTime("preferred_time_end"));

                    System.out.println("Status: "
                            + rs.getString("request_status"));

                    System.out.println("-------------------------");
                }

                if (!found) {
                    System.out.println("You have no service requests.");
                }
            }

        } catch (SQLException e) {
            System.out.println("Could not retrieve your requests.");
            e.printStackTrace();
        }
    }

    // CUSTOMER: Confirm whether the service was completed successfully
    public static void confirmServiceCompletion(
            Scanner scanner, int customerId) {

        System.out.print("Enter Request ID: ");

        int requestId;

        try {
            requestId = Integer.parseInt(scanner.nextLine());

            if (requestId <= 0) {
                System.out.println("Invalid Request ID.");
                return;
            }

        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid Request ID.");
            return;
        }

        System.out.println("\nWas your problem solved?");
        System.out.println("1. Yes, confirm completion");
        System.out.println("2. No, request a revisit");
        System.out.print("Enter your choice: ");

        String choice = scanner.nextLine();

        String newStatus;

        if (choice.equals("1")) {
            newStatus = "COMPLETED";
        } else if (choice.equals("2")) {
            newStatus = "REVISIT_PENDING";
        } else {
            System.out.println("Invalid choice.");
            return;
        }

        String sql = "UPDATE service_requests "
                + "SET request_status = ? "
                + "WHERE request_id = ? "
                + "AND customer_id = ? "
                + "AND request_status = 'WORKER_COMPLETED'";

        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, newStatus);
            ps.setInt(2, requestId);
            ps.setInt(3, customerId);

            int rowsUpdated = ps.executeUpdate();

            if (rowsUpdated > 0) {

                if (newStatus.equals("COMPLETED")) {
                    System.out.println(
                            "Service completion confirmed successfully!");
                } else {
                    System.out.println(
                            "Revisit requested successfully!");
                    System.out.println(
                            "Waiting for the worker to propose a new appointment.");
                }

            } else {
                System.out.println(
                        "Request not found, not yours, "
                                + "or not awaiting confirmation.");
            }

        } catch (SQLException e) {
            System.out.println("Could not update request status.");
            e.printStackTrace();
        }
    }

    // WORKER: View requests where customers have requested a revisit
    public static void viewRevisitRequests(int workerId) {

        String sql = "SELECT request_id, customer_id, service_name, "
                + "service_description, revisit_date, "
                + "request_status "
                + "FROM service_requests "
                + "WHERE worker_id = ? "
                + "AND request_status = 'REVISIT_PENDING' "
                + "ORDER BY created_at DESC";

        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, workerId);

            try (ResultSet rs = ps.executeQuery()) {

                boolean found = false;

                while (rs.next()) {
                    found = true;

                    System.out.println("\nRequest ID: "
                            + rs.getInt("request_id"));

                    System.out.println("Customer ID: "
                            + rs.getInt("customer_id"));

                    System.out.println("Service: "
                            + rs.getString("service_name"));

                    System.out.println("Problem: "
                            + rs.getString("service_description"));

                    System.out.println("Revisit Date: "
                            + rs.getDate("revisit_date"));

                    System.out.println("Status: "
                            + rs.getString("request_status"));

                    System.out.println("-------------------------");
                }

                if (!found) {
                    System.out.println("No pending revisit requests.");
                }
            }

        } catch (SQLException e) {
            System.out.println("Could not retrieve revisit requests.");
            e.printStackTrace();
        }
    }

    // WORKER: Propose a new appointment for a revisit
    public static void proposeRevisitAppointment(
            Scanner scanner, int workerId) {

        System.out.print("Enter Request ID: ");

        int requestId;

        try {
            requestId = Integer.parseInt(scanner.nextLine());

            if (requestId <= 0) {
                System.out.println("Invalid Request ID.");
                return;
            }

        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid Request ID.");
            return;
        }

        System.out.print(
                "Enter proposed revisit date (YYYY-MM-DD, DD/MM/YYYY or DD-MM-YYYY): ");
        String dateInput = scanner.nextLine().trim();

        System.out.print("Enter proposed start time (HH:MM): ");
        String startInput = scanner.nextLine().trim();

        System.out.print("Enter proposed end time (HH:MM): ");
        String endInput = scanner.nextLine().trim();

        LocalDate revisitDate;
        LocalTime startTime;
        LocalTime endTime;

        try {
            DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                    .appendPattern("[yyyy-MM-dd][dd/MM/yyyy][dd-MM-yyyy]")
                    .toFormatter();

            revisitDate = LocalDate.parse(dateInput, formatter);
            startTime = LocalTime.parse(startInput);
            endTime = LocalTime.parse(endInput);

            if (!revisitDate.isAfter(LocalDate.now())) {
                System.out.println("Revisit date must be in the future.");
                return;
            }

            if (!endTime.isAfter(startTime)) {
                System.out.println("End time must be later than start time.");
                return;
            }

        } catch (DateTimeParseException e) {
            System.out.println("Invalid date or time format.");
            return;
        }

        String sql = "UPDATE service_requests "
                + "SET revisit_date = ?, "
                + "revisit_time_start = ?, "
                + "revisit_time_end = ?, "
                + "request_status = 'REVISIT_PROPOSED' "
                + "WHERE request_id = ? "
                + "AND worker_id = ? "
                + "AND request_status = 'REVISIT_PENDING'";

        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setDate(1, java.sql.Date.valueOf(revisitDate));
            ps.setTime(2, java.sql.Time.valueOf(startTime));
            ps.setTime(3, java.sql.Time.valueOf(endTime));
            ps.setInt(4, requestId);
            ps.setInt(5, workerId);

            int rowsUpdated = ps.executeUpdate();

            if (rowsUpdated > 0) {
                System.out.println(
                        "Revisit appointment proposed successfully!");
                System.out.println("Waiting for customer approval.");
            } else {
                System.out.println(
                        "Request not found, not assigned to you, "
                                + "or not awaiting a revisit proposal.");
            }

        } catch (SQLException e) {
            System.out.println("Could not propose revisit appointment.");
            e.printStackTrace();
        }
    }

    // CUSTOMER: View worker's proposed revisit appointments
    public static void viewProposedRevisits(int customerId) {

        String sql = "SELECT request_id, worker_id, service_name, "
                + "revisit_date, revisit_time_start, revisit_time_end "
                + "FROM service_requests "
                + "WHERE customer_id = ? "
                + "AND request_status = 'REVISIT_PROPOSED'";

        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, customerId);

            try (ResultSet rs = ps.executeQuery()) {

                boolean found = false;

                while (rs.next()) {
                    found = true;

                    System.out.println("\nRequest ID: "
                            + rs.getInt("request_id"));

                    System.out.println("Worker ID: "
                            + rs.getInt("worker_id"));

                    System.out.println("Service: "
                            + rs.getString("service_name"));

                    System.out.println("Proposed Date: "
                            + rs.getDate("revisit_date"));

                    System.out.println("Start Time: "
                            + rs.getTime("revisit_time_start"));

                    System.out.println("End Time: "
                            + rs.getTime("revisit_time_end"));

                    System.out.println("-------------------------");
                }

                if (!found) {
                    System.out.println("No revisit proposals available.");
                }
            }

        } catch (SQLException e) {
            System.out.println("Could not retrieve revisit proposals.");
            e.printStackTrace();
        }
    }

    // CUSTOMER: Approve or reject a proposed revisit appointment
    public static void respondToRevisitProposal(
            Scanner scanner, int customerId) {

        System.out.print("Enter Request ID: ");

        int requestId;

        try {
            requestId = Integer.parseInt(scanner.nextLine());

            if (requestId <= 0) {
                System.out.println("Invalid Request ID.");
                return;
            }

        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid Request ID.");
            return;
        }

        System.out.println("\n1. Approve Revisit Appointment");
        System.out.println("2. Reject Revisit Appointment");
        System.out.print("Enter choice: ");

        String choice = scanner.nextLine().trim();

        String newStatus;

        if (choice.equals("1")) {
            newStatus = "REVISIT_SCHEDULED";
        } else if (choice.equals("2")) {
            newStatus = "REVISIT_PENDING";
        } else {
            System.out.println("Invalid choice.");
            return;
        }

        String sql = "UPDATE service_requests "
                + "SET request_status = ?, "
                + "revisit_date = CASE WHEN ? = 'REVISIT_PENDING' "
                + "THEN NULL ELSE revisit_date END, "
                + "revisit_time_start = CASE WHEN ? = 'REVISIT_PENDING' "
                + "THEN NULL ELSE revisit_time_start END, "
                + "revisit_time_end = CASE WHEN ? = 'REVISIT_PENDING' "
                + "THEN NULL ELSE revisit_time_end END "
                + "WHERE request_id = ? "
                + "AND customer_id = ? "
                + "AND request_status = 'REVISIT_PROPOSED'";
        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, newStatus);
            ps.setString(2, newStatus);
            ps.setString(3, newStatus);
            ps.setString(4, newStatus);
            ps.setInt(5, requestId);
            ps.setInt(6, customerId);

            int rowsUpdated = ps.executeUpdate();

            if (rowsUpdated > 0) {
                if (choice.equals("1")) {
                    System.out.println(
                            "Revisit appointment approved successfully!");
                } else {
                    System.out.println(
                            "Proposal rejected. Worker can suggest another date.");
                }
            } else {
                System.out.println(
                        "Proposal not found or does not belong to you.");
            }

        } catch (SQLException e) {
            System.out.println("Could not update revisit proposal.");
            e.printStackTrace();
        }
    }

    // CUSTOMER: Submit a review after successful service completion
    public static void submitReview(Scanner scanner, int customerId) {

        System.out.print("Enter Request ID: ");

        int requestId;

        try {
            requestId = Integer.parseInt(scanner.nextLine());

            if (requestId <= 0) {
                System.out.println("Invalid Request ID.");
                return;
            }

        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid Request ID.");
            return;
        }

        System.out.print("Give a rating (1-5): ");

        int rating;

        try {
            rating = Integer.parseInt(scanner.nextLine());

            if (rating < 1 || rating > 5) {
                System.out.println("Rating must be between 1 and 5.");
                return;
            }

        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid rating.");
            return;
        }

        System.out.print("Write your feedback: ");
        String comment = scanner.nextLine().trim();

        if (comment.length() > 500) {
            System.out.println("Feedback cannot exceed 500 characters.");
            return;
        }

        String sql = "INSERT INTO reviews "
                + "(request_id, customer_id, worker_id, rating, comment) "
                + "SELECT request_id, customer_id, worker_id, ?, ? "
                + "FROM service_requests "
                + "WHERE request_id = ? "
                + "AND customer_id = ? "
                + "AND request_status = 'COMPLETED'";

        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, rating);
            ps.setString(2, comment.isEmpty() ? null : comment);
            ps.setInt(3, requestId);
            ps.setInt(4, customerId);

            int rowsInserted = ps.executeUpdate();

            if (rowsInserted > 0) {
                System.out.println("Review submitted successfully!");
            } else {
                System.out.println(
                        "Request not found, not yours, or not completed.");
            }

        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                System.out.println("You have already reviewed this request.");
            } else {
                System.out.println("Could not submit review.");
                e.printStackTrace();
            }
        }
    }

    // CUSTOMER: Submit a report against a worker
    public static void submitReport(Scanner scanner, int customerId) {

        System.out.print("Enter Request ID: ");

        int requestId;

        try {
            requestId = Integer.parseInt(scanner.nextLine());

            if (requestId <= 0) {
                System.out.println("Invalid Request ID.");
                return;
            }

        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid Request ID.");
            return;
        }

        System.out.print("Enter report reason: ");
        String reason = scanner.nextLine().trim();

        if (reason.isEmpty() || reason.length() > 100) {
            System.out.println("Reason must contain 1-100 characters.");
            return;
        }

        System.out.print("Describe the problem: ");
        String description = scanner.nextLine().trim();

        if (description.isEmpty() || description.length() > 500) {
            System.out.println("Description must contain 1-500 characters.");
            return;
        }

        String sql = "INSERT INTO reports "
                + "(reporter_id, reported_user_id, request_id, "
                + "report_reason, report_description) "
                + "SELECT sr.customer_id, w.user_id, sr.request_id, ?, ? "
                + "FROM service_requests sr "
                + "JOIN workers w ON sr.worker_id = w.worker_id "
                + "WHERE sr.request_id = ? "
                + "AND sr.customer_id = ?";

        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, reason);
            ps.setString(2, description);
            ps.setInt(3, requestId);
            ps.setInt(4, customerId);

            int rowsInserted = ps.executeUpdate();

            if (rowsInserted > 0) {
                System.out.println("Report submitted successfully!");
                System.out.println("Your report is pending admin review.");
            } else {
                System.out.println(
                        "Request not found or does not belong to you.");
            }

        } catch (SQLException e) {

            if (e.getErrorCode() == 1062) {
                System.out.println(
                        "A report has already been submitted for this request.");
            } else {
                System.out.println("Could not submit report.");
                e.printStackTrace();
            }
        }
    }

    // CUSTOMER: View status of submitted reports
    public static void viewMyReports(int customerId) {

        String sql = "SELECT report_id, request_id, report_reason, "
                + "report_status, admin_remarks, created_at "
                + "FROM reports "
                + "WHERE reporter_id = ? "
                + "ORDER BY created_at DESC";

        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, customerId);

            try (ResultSet rs = ps.executeQuery()) {

                boolean found = false;

                while (rs.next()) {

                    found = true;

                    System.out.println("\n===== MY REPORT =====");
                    System.out.println("Report ID: "
                            + rs.getInt("report_id"));
                    System.out.println("Request ID: "
                            + rs.getInt("request_id"));
                    System.out.println("Reason: "
                            + rs.getString("report_reason"));
                    System.out.println("Status: "
                            + rs.getString("report_status"));

                    String remarks = rs.getString("admin_remarks");

                    if (remarks == null) {
                        remarks = "No admin remarks yet.";
                    }

                    System.out.println("Admin Remarks: " + remarks);
                    System.out.println("Submitted At: "
                            + rs.getTimestamp("created_at"));
                    System.out.println("======================");
                }

                if (!found) {
                    System.out.println("You have not submitted any reports.");
                }
            }

        } catch (SQLException e) {
            System.out.println("Could not retrieve your reports.");
            e.printStackTrace();
        }
    }

    // WEB: Create a service request
    public static boolean createWebRequest(
            int customerId,
            int workerId,
            int priceId,
            String serviceDescription,
            String appointmentDate,
            String startTime,
            String endTime) {

        if (serviceDescription == null
                || serviceDescription.trim().isEmpty()
                || serviceDescription.trim().length() > 1000) {
            System.out.println("BOOKING FAILED: Invalid description");
            return false;
        }

        try {
            java.time.LocalDate date = java.time.LocalDate.parse(appointmentDate);
            java.time.LocalTime start = java.time.LocalTime.parse(startTime);
            java.time.LocalTime end = java.time.LocalTime.parse(endTime);

            System.out.println("Booking date: " + date);
            System.out.println("Booking day: " + date.getDayOfWeek());
            System.out.println("Start time: " + start);
            System.out.println("End time: " + end);

            if (!date.isAfter(java.time.LocalDate.now())) {
                System.out.println("BOOKING FAILED: Appointment date is not in the future.");
                return false;
            }

            if (!end.isAfter(start)) {
                System.out.println("BOOKING FAILED: End time must be after start time.");
                return false;
            }

            try (Connection con = DatabaseConnection.getConnection()) {
                con.setAutoCommit(false);

                try {
                    String workerSql = "SELECT worker_id FROM workers w "
                            + "JOIN users u ON w.user_id = u.user_id "
                            + "WHERE w.worker_id = ? "
                            + "AND w.verification_status = 'APPROVED' "
                            + "AND u.account_status = 'ACTIVE'";

                    try (PreparedStatement ps = con.prepareStatement(workerSql)) {
                        ps.setInt(1, workerId);

                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) {
                                System.out.println("BOOKING FAILED: Worker is not approved or active.");
                                con.rollback();
                                return false;
                            }
                        }
                    }

                    String priceSql = "SELECT service_name, service_price, inspection_fee "
                            + "FROM worker_service_prices "
                            + "WHERE price_id = ? AND worker_id = ?";

                    String serviceName;
                    java.math.BigDecimal servicePrice;
                    java.math.BigDecimal inspectionFee;

                    try (PreparedStatement ps = con.prepareStatement(priceSql)) {
                        ps.setInt(1, priceId);
                        ps.setInt(2, workerId);

                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) {
                                System.out.println("BOOKING FAILED: Invalid price ID or worker ID.");
                                con.rollback();
                                return false;
                            }

                            serviceName = rs.getString("service_name");
                            servicePrice = rs.getBigDecimal("service_price");
                            inspectionFee = rs.getBigDecimal("inspection_fee");
                        }
                    }

                    String availabilitySql = "SELECT availability_id FROM worker_availability "
                            + "WHERE worker_id = ? AND available_day = ? "
                            + "AND is_available = 1 "
                            + "AND start_time <= ? AND end_time >= ?";

                    try (PreparedStatement ps = con.prepareStatement(availabilitySql)) {
                        ps.setInt(1, workerId);
                        ps.setString(2, date.getDayOfWeek().name());
                        ps.setTime(3, java.sql.Time.valueOf(end));
                        ps.setTime(4, java.sql.Time.valueOf(start));

                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) {
                                System.out.println("BOOKING FAILED: Worker is not available on this day or time.");
                                con.rollback();
                                return false;
                            }
                        }
                    }

                    String overlapSql = "SELECT request_id FROM service_requests "
                            + "WHERE worker_id = ? AND appointment_date = ? "
                            + "AND request_status IN ('PENDING', 'ACCEPTED', 'DISPUTED') "
                            + "AND preferred_time_start < ? "
                            + "AND preferred_time_end > ?";

                    try (PreparedStatement ps = con.prepareStatement(overlapSql)) {
                        ps.setInt(1, workerId);
                        ps.setDate(2, java.sql.Date.valueOf(date));
                        ps.setTime(3, java.sql.Time.valueOf(end));
                        ps.setTime(4, java.sql.Time.valueOf(start));

                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                System.out.println("BOOKING FAILED: Another booking overlaps this time.");
                                con.rollback();
                                return false;
                            }
                        }
                    }

                    String insertSql = "INSERT INTO service_requests "
                            + "(customer_id, worker_id, service_name, service_description, "
                            + "service_price, inspection_fee, appointment_date, "
                            + "preferred_time_start, preferred_time_end) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

                    try (PreparedStatement ps = con.prepareStatement(insertSql)) {
                        ps.setInt(1, customerId);
                        ps.setInt(2, workerId);
                        ps.setString(3, serviceName);
                        ps.setString(4, serviceDescription.trim());
                        ps.setBigDecimal(5, servicePrice);
                        ps.setBigDecimal(6, inspectionFee);
                        ps.setDate(7, java.sql.Date.valueOf(date));
                        ps.setTime(8, java.sql.Time.valueOf(start));
                        ps.setTime(9, java.sql.Time.valueOf(end));

                        ps.executeUpdate();
                    }

                    con.commit();
                    System.out.println("BOOKING SUCCESSFUL!");
                    return true;

                } catch (Exception e) {
                    con.rollback();
                    throw e;
                } finally {
                    con.setAutoCommit(true);
                }
            }

        } catch (Exception e) {
            System.out.println("WEB BOOKING ERROR:");
            e.printStackTrace();
            return false;
        }
    }

    // WEB: Submit a review for a completed service request
    public static boolean submitWebReview(
            int customerId, int requestId, int rating, String comment) {

        if (requestId <= 0 || rating < 1 || rating > 5) {
            return false;
        }

        if (comment != null && comment.length() > 500) {
            return false;
        }

        String sql = "INSERT INTO reviews "
                + "(request_id, customer_id, worker_id, rating, comment) "
                + "SELECT request_id, customer_id, worker_id, ?, ? "
                + "FROM service_requests "
                + "WHERE request_id = ? "
                + "AND customer_id = ? "
                + "AND request_status = 'COMPLETED'";

        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, rating);
            ps.setString(2, comment == null || comment.trim().isEmpty()
                    ? null
                    : comment.trim());
            ps.setInt(3, requestId);
            ps.setInt(4, customerId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // WEB: Get reviews displayed on a worker's profile
    public static List<Map<String, Object>> getWorkerReviewsForApi(
            int workerId) {

        List<Map<String, Object>> reviews = new ArrayList<>();

        String sql = "SELECT r.review_id, r.request_id, r.rating, "
                + "r.comment, r.created_at, u.name AS customer_name "
                + "FROM reviews r "
                + "JOIN users u ON r.customer_id = u.user_id "
                + "WHERE r.worker_id = ? "
                + "ORDER BY r.created_at DESC";

        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, workerId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> review = new HashMap<>();

                    review.put("review_id", rs.getInt("review_id"));
                    review.put("request_id", rs.getInt("request_id"));
                    review.put("rating", rs.getInt("rating"));
                    review.put("comment", rs.getString("comment"));
                    review.put("created_at", rs.getTimestamp("created_at"));
                    review.put("customer_name", rs.getString("customer_name"));

                    reviews.add(review);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not load worker reviews.", e);
        }

        return reviews;
    }

    // WEB: Get incoming requests for the logged-in worker
    public static java.util.List<java.util.Map<String, Object>> getWorkerRequestsForApi(
            int userId) {

        java.util.List<java.util.Map<String, Object>> requests = new java.util.ArrayList<>();

        String sql = "SELECT w.worker_id, sr.request_id, sr.customer_id, cu.name AS customer_name, "
                + "sr.service_name, sr.service_description, sr.service_price, "
                + "sr.inspection_fee, sr.parts_cost, sr.parts_description, "
                + "sr.parts_status, sr.appointment_date, "
                + "sr.preferred_time_start, sr.preferred_time_end, "
                + "sr.request_status "
                + "FROM service_requests sr "
                + "JOIN workers w ON sr.worker_id = w.worker_id "
                + "JOIN users cu ON sr.customer_id = cu.user_id "
                + "WHERE w.user_id = ? "
                + "ORDER BY sr.created_at DESC";

        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    java.util.Map<String, Object> request = new java.util.HashMap<>();
                    request.put("worker_id", rs.getInt("worker_id"));
                    request.put("request_id", rs.getInt("request_id"));
                    request.put("customer_id", rs.getInt("customer_id"));
                    request.put("customer_name", rs.getString("customer_name"));
                    request.put("service_name", rs.getString("service_name"));
                    request.put("service_description",
                            rs.getString("service_description"));
                    request.put("service_price", rs.getBigDecimal("service_price"));
                    request.put("inspection_fee", rs.getBigDecimal("inspection_fee"));
                    request.put("parts_cost", rs.getBigDecimal("parts_cost"));
                    request.put("parts_description",
                            rs.getString("parts_description"));
                    request.put("parts_status",
                            rs.getString("parts_status"));
                    request.put("appointment_date", rs.getDate("appointment_date"));
                    request.put("preferred_time_start",
                            rs.getTime("preferred_time_start"));
                    request.put("preferred_time_end",
                            rs.getTime("preferred_time_end"));
                    request.put("request_status",
                            rs.getString("request_status"));

                    requests.add(request);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not load worker requests.", e);
        }

        return requests;
    }

    // WEB: Accept or reject a request by the logged-in worker
    public static boolean updateWebRequestStatus(
            int userId, int requestId, String newStatus) {

        if (!"ACCEPTED".equals(newStatus)
                && !"REJECTED".equals(newStatus)) {
            return false;
        }

        String sql = "UPDATE service_requests sr "
                + "JOIN workers w ON sr.worker_id = w.worker_id "
                + "SET sr.request_status = ? "
                + "WHERE sr.request_id = ? "
                + "AND w.user_id = ? "
                + "AND sr.request_status = 'PENDING'";

        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, newStatus);
            ps.setInt(2, requestId);
            ps.setInt(3, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Could not update web request status.");
            e.printStackTrace();
            return false;
        }
    }

    public static int getWorkerIdByUserId(int userId) {

        String sql = "SELECT worker_id FROM workers WHERE user_id = ?";

        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("worker_id");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not find worker ID.", e);
        }

        return -1;
    }

    // WEB: Submit a report against a worker
    public static boolean submitWebReport(
            int customerId,
            int requestId,
            String reason,
            String description) {

        if (requestId <= 0
                || reason == null || reason.trim().isEmpty()
                || reason.trim().length() > 100
                || description == null || description.trim().isEmpty()
                || description.trim().length() > 500) {
            return false;
        }

        String sql = "INSERT INTO reports "
                + "(reporter_id, reported_user_id, request_id, "
                + "report_reason, report_description) "
                + "SELECT sr.customer_id, w.user_id, sr.request_id, ?, ? "
                + "FROM service_requests sr "
                + "JOIN workers w ON sr.worker_id = w.worker_id "
                + "WHERE sr.request_id = ? "
                + "AND sr.customer_id = ?";

        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, reason.trim());
            ps.setString(2, description.trim());
            ps.setInt(3, requestId);
            ps.setInt(4, customerId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // WEB: Get reports submitted by the logged-in customer
    public static List<Map<String, Object>> getMyReportsForApi(
            int customerId) {

        List<Map<String, Object>> reports = new ArrayList<>();

        String sql = "SELECT report_id, request_id, report_reason, "
                + "report_description, report_status, admin_remarks, created_at "
                + "FROM reports "
                + "WHERE reporter_id = ? "
                + "ORDER BY created_at DESC";

        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, customerId);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    Map<String, Object> report = new HashMap<>();

                    report.put("report_id", rs.getInt("report_id"));
                    report.put("request_id", rs.getObject("request_id"));
                    report.put("report_reason", rs.getString("report_reason"));
                    report.put("report_description",
                            rs.getString("report_description"));
                    report.put("report_status",
                            rs.getString("report_status"));
                    report.put("admin_remarks",
                            rs.getString("admin_remarks"));
                    report.put("created_at",
                            rs.getTimestamp("created_at"));

                    reports.add(report);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not load customer reports.", e);
        }

        return reports;
    }

    // WEB: Get proposed revisit appointments for the logged-in customer
    public static List<Map<String, Object>> getProposedRevisitsForApi(
            int customerId) {

        List<Map<String, Object>> revisits = new ArrayList<>();

        String sql = "SELECT request_id, worker_id, service_name, "
                + "revisit_date, revisit_time_start, revisit_time_end "
                + "FROM service_requests "
                + "WHERE customer_id = ? "
                + "AND request_status = 'REVISIT_PROPOSED' "
                + "ORDER BY revisit_date ASC";

        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, customerId);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    Map<String, Object> revisit = new HashMap<>();

                    revisit.put("request_id", rs.getInt("request_id"));
                    revisit.put("worker_id", rs.getInt("worker_id"));
                    revisit.put("service_name", rs.getString("service_name"));
                    revisit.put("revisit_date", rs.getDate("revisit_date"));
                    revisit.put("revisit_time_start",
                            rs.getTime("revisit_time_start"));
                    revisit.put("revisit_time_end",
                            rs.getTime("revisit_time_end"));

                    revisits.add(revisit);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(
                    "Could not load proposed revisit appointments.", e);
        }

        return revisits;
    }

    // WEB: Customer approves or rejects a proposed revisit appointment
    public static boolean respondToRevisitProposalForApi(
            int customerId, int requestId, boolean approve) {

        String newStatus = approve
                ? "REVISIT_SCHEDULED"
                : "REVISIT_PENDING";

        String sql = "UPDATE service_requests "
                + "SET request_status = ?, "
                + "revisit_date = CASE WHEN ? = 'REVISIT_PENDING' "
                + "THEN NULL ELSE revisit_date END, "
                + "revisit_time_start = CASE WHEN ? = 'REVISIT_PENDING' "
                + "THEN NULL ELSE revisit_time_start END, "
                + "revisit_time_end = CASE WHEN ? = 'REVISIT_PENDING' "
                + "THEN NULL ELSE revisit_time_end END "
                + "WHERE request_id = ? "
                + "AND customer_id = ? "
                + "AND request_status = 'REVISIT_PROPOSED'";

        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, newStatus);
            ps.setString(2, newStatus);
            ps.setString(3, newStatus);
            ps.setString(4, newStatus);
            ps.setInt(5, requestId);
            ps.setInt(6, customerId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(
                    "Could not respond to revisit proposal.", e);
        }
    }

    // WEB: Get pending revisit requests for the logged-in worker
    public static List<Map<String, Object>> getPendingRevisitsForApi(
            int userId) {

        List<Map<String, Object>> revisits = new ArrayList<>();

        String sql = "SELECT sr.request_id, sr.customer_id, "
                + "cu.name AS customer_name, sr.service_name, "
                + "sr.service_description, sr.appointment_date "
                + "FROM service_requests sr "
                + "JOIN workers w ON sr.worker_id = w.worker_id "
                + "JOIN users cu ON sr.customer_id = cu.user_id "
                + "WHERE w.user_id = ? "
                + "AND sr.request_status = 'REVISIT_PENDING' "
                + "ORDER BY sr.created_at DESC";

        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    Map<String, Object> revisit = new HashMap<>();

                    revisit.put("request_id", rs.getInt("request_id"));
                    revisit.put("customer_id", rs.getInt("customer_id"));
                    revisit.put("customer_name",
                            rs.getString("customer_name"));
                    revisit.put("service_name",
                            rs.getString("service_name"));
                    revisit.put("service_description",
                            rs.getString("service_description"));
                    revisit.put("appointment_date",
                            rs.getDate("appointment_date"));

                    revisits.add(revisit);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(
                    "Could not load pending revisit requests.", e);
        }

        return revisits;
    }

    // WEB: Worker proposes a new revisit appointment
    public static boolean proposeRevisitAppointmentForApi(
            int userId,
            int requestId,
            String dateInput,
            String startInput,
            String endInput) {

        if (requestId <= 0
                || dateInput == null
                || startInput == null
                || endInput == null) {
            return false;
        }

        try {
            LocalDate revisitDate = LocalDate.parse(dateInput);
            LocalTime startTime = LocalTime.parse(startInput);
            LocalTime endTime = LocalTime.parse(endInput);

            if (!revisitDate.isAfter(LocalDate.now())
                    || !endTime.isAfter(startTime)) {
                return false;
            }

            String sql = "UPDATE service_requests sr "
                    + "JOIN workers w ON sr.worker_id = w.worker_id "
                    + "SET sr.revisit_date = ?, "
                    + "sr.revisit_time_start = ?, "
                    + "sr.revisit_time_end = ?, "
                    + "sr.request_status = 'REVISIT_PROPOSED' "
                    + "WHERE sr.request_id = ? "
                    + "AND w.user_id = ? "
                    + "AND sr.request_status = 'REVISIT_PENDING'";

            try (Connection con = DatabaseConnection.getConnection();
                    PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setDate(1, java.sql.Date.valueOf(revisitDate));
                ps.setTime(2, java.sql.Time.valueOf(startTime));
                ps.setTime(3, java.sql.Time.valueOf(endTime));
                ps.setInt(4, requestId);
                ps.setInt(5, userId);

                return ps.executeUpdate() > 0;
            }

        } catch (DateTimeParseException e) {
            return false;

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(
                    "Could not propose revisit appointment.", e);
        }
    }

}
