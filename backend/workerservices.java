
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;
import java.math.BigDecimal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class workerservices {
        public static int getWorkerId(int userId) {

                String sql = "SELECT worker_id FROM workers WHERE user_id = ?";

                try (Connection conn = DatabaseConnection.getConnection();
                                PreparedStatement stmt = conn.prepareStatement(sql)) {

                        stmt.setInt(1, userId);

                        try (ResultSet rs = stmt.executeQuery()) {

                                if (rs.next()) {
                                        return rs.getInt("worker_id");
                                }
                        }

                } catch (SQLException e) {
                        System.out.println("Error finding worker ID: " + e.getMessage());
                }

                return -1;
        }

        public static void addServicePrice(Scanner scanner, int workerId) {

                System.out.print("Enter service name: ");
                String serviceName = scanner.nextLine().trim();

                if (serviceName.isEmpty()) {
                        System.out.println("Service name cannot be empty.");
                        return;
                }

                System.out.print("Enter service price: ");

                BigDecimal servicePrice;
                try {
                        servicePrice = new BigDecimal(scanner.nextLine());

                        if (servicePrice.compareTo(BigDecimal.ZERO) <= 0) {
                                System.out.println("Service price must be greater than zero.");
                                return;
                        }

                } catch (NumberFormatException e) {
                        System.out.println("Enter a valid price.");
                        return;
                }

                System.out.print("Enter inspection fee (enter 0 if free): ");

                BigDecimal inspectionFee;
                try {
                        inspectionFee = new BigDecimal(scanner.nextLine());

                        if (inspectionFee.compareTo(BigDecimal.ZERO) < 0) {
                                System.out.println("Inspection fee cannot be negative.");
                                return;
                        }

                } catch (NumberFormatException e) {
                        System.out.println("Enter a valid inspection fee.");
                        return;
                }

                String sql = "INSERT INTO worker_service_prices "
                                + "(worker_id, service_name, service_price, inspection_fee) "
                                + "VALUES (?, ?, ?, ?) "
                                + "ON DUPLICATE KEY UPDATE "
                                + "service_price = VALUES(service_price), "
                                + "inspection_fee = VALUES(inspection_fee)";

                try (Connection conn = DatabaseConnection.getConnection();
                                PreparedStatement stmt = conn.prepareStatement(sql)) {

                        stmt.setInt(1, workerId);
                        stmt.setString(2, serviceName);
                        stmt.setBigDecimal(3, servicePrice);
                        stmt.setBigDecimal(4, inspectionFee);

                        int rows = stmt.executeUpdate();

                        if (rows > 0) {
                                System.out.println("Service price saved successfully!");
                        }

                } catch (SQLException e) {
                        System.out.println("Error saving service price: " + e.getMessage());
                }
        }

        public static void viewMyServicePrices(int workerId) {

                String sql = "SELECT service_name, service_price, inspection_fee "
                                + "FROM worker_service_prices "
                                + "WHERE worker_id = ?";

                try (Connection conn = DatabaseConnection.getConnection();
                                PreparedStatement stmt = conn.prepareStatement(sql)) {

                        stmt.setInt(1, workerId);

                        try (ResultSet rs = stmt.executeQuery()) {

                                boolean found = false;

                                while (rs.next()) {
                                        found = true;

                                        System.out.println("\nService: "
                                                        + rs.getString("service_name"));

                                        System.out.println("Service Price: Rs. "
                                                        + rs.getBigDecimal("service_price"));

                                        System.out.println("Inspection Fee: Rs. "
                                                        + rs.getBigDecimal("inspection_fee"));
                                        System.out.println("-------------------------");
                                }

                                if (!found) {
                                        System.out.println("You have not added any service prices yet.");
                                }
                        }

                } catch (SQLException e) {
                        System.out.println("Error viewing service prices: " + e.getMessage());
                }
        }

        public static List<Map<String, Object>> getWorkerPricesForApi(int userId)
                        throws SQLException {

                List<Map<String, Object>> prices = new ArrayList<>();

                String sql = "SELECT wsp.price_id, wsp.service_name, "
                                + "wsp.service_price, wsp.inspection_fee "
                                + "FROM worker_service_prices wsp "
                                + "JOIN workers w ON wsp.worker_id = w.worker_id "
                                + "WHERE w.user_id = ? "
                                + "ORDER BY wsp.service_name";

                try (Connection conn = DatabaseConnection.getConnection();
                                PreparedStatement stmt = conn.prepareStatement(sql)) {

                        stmt.setInt(1, userId);

                        try (ResultSet rs = stmt.executeQuery()) {

                                while (rs.next()) {
                                        Map<String, Object> price = new HashMap<>();

                                        price.put("priceId", rs.getInt("price_id"));
                                        price.put("serviceName", rs.getString("service_name"));
                                        price.put("servicePrice", rs.getBigDecimal("service_price"));
                                        price.put("inspectionFee", rs.getBigDecimal("inspection_fee"));

                                        prices.add(price);
                                }
                        }
                }

                return prices;
        }

        public static boolean saveServicePrice(
                        int userId,
                        String serviceName,
                        BigDecimal servicePrice,
                        BigDecimal inspectionFee) throws SQLException {

                if (serviceName == null || serviceName.trim().isEmpty()) {
                        throw new IllegalArgumentException("Service name is required.");
                }

                if (servicePrice == null ||
                                servicePrice.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new IllegalArgumentException(
                                        "Service price must be greater than zero.");
                }

                if (inspectionFee == null ||
                                inspectionFee.compareTo(BigDecimal.ZERO) < 0) {
                        throw new IllegalArgumentException(
                                        "Inspection fee cannot be negative.");
                }

                String sql = "INSERT INTO worker_service_prices " +
                                "(worker_id, service_name, service_price, inspection_fee) " +
                                "SELECT worker_id, ?, ?, ? FROM workers WHERE user_id = ? " +
                                "ON DUPLICATE KEY UPDATE " +
                                "service_price = VALUES(service_price), " +
                                "inspection_fee = VALUES(inspection_fee)";

                try (Connection conn = DatabaseConnection.getConnection();
                                PreparedStatement stmt = conn.prepareStatement(sql)) {

                        stmt.setString(1, serviceName.trim());
                        stmt.setBigDecimal(2, servicePrice);
                        stmt.setBigDecimal(3, inspectionFee);
                        stmt.setInt(4, userId);

                        return stmt.executeUpdate() > 0;
                }
        }

        public static boolean updateServicePrice(
                        int userId,
                        int priceId,
                        String serviceName,
                        BigDecimal servicePrice,
                        BigDecimal inspectionFee) throws SQLException {

                if (priceId <= 0) {
                        throw new IllegalArgumentException("Invalid price ID.");
                }

                if (serviceName == null || serviceName.trim().isEmpty()) {
                        throw new IllegalArgumentException("Service name is required.");
                }

                if (servicePrice == null ||
                                servicePrice.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new IllegalArgumentException(
                                        "Service price must be greater than zero.");
                }

                if (inspectionFee == null ||
                                inspectionFee.compareTo(BigDecimal.ZERO) < 0) {
                        throw new IllegalArgumentException(
                                        "Inspection fee cannot be negative.");
                }

                String sql = "UPDATE worker_service_prices wsp " +
                                "JOIN workers w ON wsp.worker_id = w.worker_id " +
                                "SET wsp.service_name = ?, " +
                                "wsp.service_price = ?, " +
                                "wsp.inspection_fee = ? " +
                                "WHERE wsp.price_id = ? AND w.user_id = ?";

                try (Connection conn = DatabaseConnection.getConnection();
                                PreparedStatement stmt = conn.prepareStatement(sql)) {

                        stmt.setString(1, serviceName.trim());
                        stmt.setBigDecimal(2, servicePrice);
                        stmt.setBigDecimal(3, inspectionFee);
                        stmt.setInt(4, priceId);
                        stmt.setInt(5, userId);

                        return stmt.executeUpdate() > 0;
                }
        }

        public static boolean deleteServicePrice(int userId, int priceId) {
                String sql = "DELETE wsp FROM worker_service_prices wsp " +
                                "JOIN workers w ON wsp.worker_id = w.worker_id " +
                                "WHERE wsp.price_id = ? AND w.user_id = ?";

                try (Connection conn = DatabaseConnection.getConnection();
                                PreparedStatement ps = conn.prepareStatement(sql)) {

                        ps.setInt(1, priceId);
                        ps.setInt(2, userId);

                        return ps.executeUpdate() > 0;

                } catch (SQLException e) {
                        e.printStackTrace();
                        return false;
                }
        }

        public static void findApprovedWorkers(String category) {

                String sql = "SELECT w.worker_id, u.name, u.phone, " +
                                "w.service_category, w.experience_years, w.description " +
                                "FROM workers w " +
                                "JOIN users u ON w.user_id = u.user_id " +
                                "WHERE w.service_category = ? " +
                                "AND w.verification_status = 'APPROVED' " +
                                "AND u.account_status = 'ACTIVE'";

                try (Connection conn = DatabaseConnection.getConnection();
                                PreparedStatement stmt = conn.prepareStatement(sql)) {

                        stmt.setString(1, category);

                        try (ResultSet rs = stmt.executeQuery()) {

                                boolean found = false;

                                while (rs.next()) {
                                        found = true;

                                        System.out.println("\nWorker ID: "
                                                        + rs.getInt("worker_id"));
                                        System.out.println("Name: "
                                                        + rs.getString("name"));
                                        System.out.println("Phone: "
                                                        + rs.getString("phone"));
                                        System.out.println("Category: "
                                                        + rs.getString("service_category"));
                                        System.out.println("Experience: "
                                                        + rs.getInt("experience_years") + " years");
                                        System.out.println("Description: "
                                                        + rs.getString("description"));
                                        // Display the worker's service prices
                                        String priceSql = "SELECT price_id, service_name, service_price, inspection_fee "
                                                        +
                                                        "FROM worker_service_prices WHERE worker_id = ?";

                                        try (PreparedStatement priceStmt = conn.prepareStatement(priceSql)) {

                                                priceStmt.setInt(1, rs.getInt("worker_id"));

                                                try (ResultSet priceRs = priceStmt.executeQuery()) {

                                                        boolean hasPrices = false;

                                                        System.out.println("\nAvailable Services:");

                                                        while (priceRs.next()) {
                                                                hasPrices = true;

                                                                System.out.println("Service: "
                                                                                + priceRs.getString("service_name"));

                                                                System.out.println("Service Price: Rs. "
                                                                                + priceRs.getBigDecimal(
                                                                                                "service_price"));

                                                                System.out.println("Inspection Fee: Rs. "
                                                                                + priceRs.getBigDecimal(
                                                                                                "inspection_fee"));
                                                        }

                                                        if (!hasPrices) {
                                                                System.out.println(
                                                                                "This worker has not added any service prices yet.");
                                                        }
                                                }
                                        }
                                        System.out.println("-------------------------");
                                }

                                if (!found) {
                                        System.out.println(
                                                        "No approved workers found for " + category + ".");
                                }
                        }

                } catch (SQLException e) {
                        System.out.println(
                                        "Error finding workers: " + e.getMessage());
                }
        }

        // WORKER: Add availability
        public static void addAvailability(Scanner scanner, int workerId) {

                System.out.println("\n===== ADD AVAILABILITY =====");

                System.out.println("MONDAY, TUESDAY, WEDNESDAY, THURSDAY");
                System.out.println("FRIDAY, SATURDAY, SUNDAY");

                System.out.print("Enter available day: ");
                String day = scanner.nextLine().trim().toUpperCase();

                String[] validDays = {
                                "MONDAY", "TUESDAY", "WEDNESDAY",
                                "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
                };

                boolean validDay = false;

                for (String valid : validDays) {
                        if (day.equals(valid)) {
                                validDay = true;
                                break;
                        }
                }

                if (!validDay) {
                        System.out.println("Invalid day.");
                        return;
                }

                System.out.print("Enter start time (HH:mm): ");
                String startTime = scanner.nextLine().trim();

                System.out.print("Enter end time (HH:mm): ");
                String endTime = scanner.nextLine().trim();

                java.sql.Time start;
                java.sql.Time end;

                try {
                        start = java.sql.Time.valueOf(startTime + ":00");
                        end = java.sql.Time.valueOf(endTime + ":00");

                        if (!start.before(end)) {
                                System.out.println(
                                                "End time must be later than start time.");
                                return;
                        }

                } catch (IllegalArgumentException e) {
                        System.out.println(
                                        "Invalid time. Use HH:mm format, such as 09:00.");
                        return;
                }

                String checkSql = "SELECT availability_id "
                                + "FROM worker_availability "
                                + "WHERE worker_id = ? "
                                + "AND available_day = ? "
                                + "AND is_available = 1 "
                                + "AND start_time < ? "
                                + "AND end_time > ?";

                String insertSql = "INSERT INTO worker_availability "
                                + "(worker_id, available_day, start_time, end_time) "
                                + "VALUES (?, ?, ?, ?)";

                try (Connection con = DatabaseConnection.getConnection();
                                PreparedStatement check = con.prepareStatement(checkSql)) {

                        check.setInt(1, workerId);
                        check.setString(2, day);
                        check.setTime(3, end);
                        check.setTime(4, start);

                        try (ResultSet rs = check.executeQuery()) {

                                if (rs.next()) {
                                        System.out.println(
                                                        "This time slot overlaps with existing availability.");
                                        return;
                                }
                        }

                        try (PreparedStatement ps = con.prepareStatement(insertSql)) {

                                ps.setInt(1, workerId);
                                ps.setString(2, day);
                                ps.setTime(3, start);
                                ps.setTime(4, end);

                                ps.executeUpdate();

                                System.out.println("Availability added successfully!");
                        }

                } catch (SQLException e) {
                        if (e.getErrorCode() == 1062) {
                                System.out.println("This availability slot already exists.");
                        } else {
                                System.out.println("Could not add availability.");
                                e.printStackTrace();
                        }
                }
        }

        // WORKER: View own availability
        public static void viewAvailability(int workerId) {

                String sql = "SELECT availability_id, available_day, "
                                + "start_time, end_time, is_available "
                                + "FROM worker_availability "
                                + "WHERE worker_id = ? "
                                + "ORDER BY FIELD(available_day, "
                                + "'MONDAY','TUESDAY','WEDNESDAY','THURSDAY',"
                                + "'FRIDAY','SATURDAY','SUNDAY'), start_time";

                try (Connection con = DatabaseConnection.getConnection();
                                PreparedStatement ps = con.prepareStatement(sql)) {

                        ps.setInt(1, workerId);

                        try (ResultSet rs = ps.executeQuery()) {

                                boolean found = false;

                                System.out.println("\n===== MY AVAILABILITY =====");

                                while (rs.next()) {
                                        found = true;

                                        System.out.println("Availability ID: "
                                                        + rs.getInt("availability_id"));

                                        System.out.println("Day: "
                                                        + rs.getString("available_day"));

                                        System.out.println("Start: "
                                                        + rs.getTime("start_time"));

                                        System.out.println("End: "
                                                        + rs.getTime("end_time"));

                                        System.out.println("Status: "
                                                        + (rs.getBoolean("is_available")
                                                                        ? "Available"
                                                                        : "Unavailable"));

                                        System.out.println("---------------------------");
                                }

                                if (!found) {
                                        System.out.println("No availability slots found.");
                                }
                        }

                } catch (SQLException e) {
                        System.out.println("Could not retrieve availability.");
                        e.printStackTrace();
                }
        }

        // WORKER: Update availability
        public static void updateAvailability(Scanner scanner, int workerId) {

                System.out.print("Enter Availability ID to update: ");

                int availabilityId;

                try {
                        availabilityId = Integer.parseInt(scanner.nextLine());

                        if (availabilityId <= 0) {
                                System.out.println("Invalid Availability ID.");
                                return;
                        }
                } catch (NumberFormatException e) {
                        System.out.println("Enter a valid Availability ID.");
                        return;
                }

                System.out.print("Enter new available day: ");
                String day = scanner.nextLine().trim().toUpperCase();

                String[] validDays = {
                                "MONDAY", "TUESDAY", "WEDNESDAY",
                                "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
                };

                boolean validDay = false;

                for (String valid : validDays) {
                        if (day.equals(valid)) {
                                validDay = true;
                                break;
                        }
                }

                if (!validDay) {
                        System.out.println("Invalid day.");
                        return;
                }

                System.out.print("Enter new start time (HH:mm): ");
                String startTime = scanner.nextLine().trim();

                System.out.print("Enter new end time (HH:mm): ");
                String endTime = scanner.nextLine().trim();

                java.sql.Time start;
                java.sql.Time end;

                try {
                        start = java.sql.Time.valueOf(startTime + ":00");
                        end = java.sql.Time.valueOf(endTime + ":00");

                        if (!start.before(end)) {
                                System.out.println("End time must be later than start time.");
                                return;
                        }

                } catch (IllegalArgumentException e) {
                        System.out.println("Invalid time format. Use HH:mm.");
                        return;
                }

                String checkSql = "SELECT availability_id "
                                + "FROM worker_availability "
                                + "WHERE worker_id = ? "
                                + "AND available_day = ? "
                                + "AND is_available = 1 "
                                + "AND start_time < ? "
                                + "AND end_time > ? "
                                + "AND availability_id <> ?";

                String updateSql = "UPDATE worker_availability "
                                + "SET available_day = ?, start_time = ?, end_time = ? "
                                + "WHERE availability_id = ? AND worker_id = ?";

                try (Connection con = DatabaseConnection.getConnection()) {

                        try (PreparedStatement check = con.prepareStatement(checkSql)) {

                                check.setInt(1, workerId);
                                check.setString(2, day);
                                check.setTime(3, end);
                                check.setTime(4, start);
                                check.setInt(5, availabilityId);

                                try (ResultSet rs = check.executeQuery()) {
                                        if (rs.next()) {
                                                System.out.println("This slot overlaps with another availability.");
                                                return;
                                        }
                                }
                        }

                        try (PreparedStatement ps = con.prepareStatement(updateSql)) {

                                ps.setString(1, day);
                                ps.setTime(2, start);
                                ps.setTime(3, end);
                                ps.setInt(4, availabilityId);
                                ps.setInt(5, workerId);

                                int rowsUpdated = ps.executeUpdate();

                                if (rowsUpdated > 0) {
                                        System.out.println("Availability updated successfully!");
                                } else {
                                        System.out.println("Availability not found or does not belong to you.");
                                }
                        }

                } catch (SQLException e) {
                        System.out.println("Could not update availability.");
                        e.printStackTrace();
                }
        }

        public static void deleteAvailability(Scanner scanner, int workerId) {

                System.out.print("Enter Availability ID to delete: ");

                if (!scanner.hasNextInt()) {
                        System.out.println("Invalid Availability ID.");
                        scanner.nextLine();
                        return;
                }

                int availabilityId = scanner.nextInt();
                scanner.nextLine();

                if (availabilityId <= 0) {
                        System.out.println("Invalid Availability ID.");
                        return;
                }

                String sql = "DELETE FROM worker_availability " +
                                "WHERE availability_id = ? AND worker_id = ?";

                try (Connection con = DatabaseConnection.getConnection();
                                PreparedStatement ps = con.prepareStatement(sql)) {

                        ps.setInt(1, availabilityId);
                        ps.setInt(2, workerId);

                        int rows = ps.executeUpdate();

                        if (rows > 0) {
                                System.out.println("Availability deleted successfully.");
                        } else {
                                System.out.println("Availability not found or you do not own it.");
                        }

                } catch (SQLException e) {
                        System.out.println("Error deleting availability: " + e.getMessage());
                }
        }

        public static List<Map<String, Object>> getApprovedWorkersForApi(String category) {

                List<Map<String, Object>> workers = new ArrayList<>();

                String sql = "SELECT w.worker_id, u.name, u.phone, " +
                                "w.service_category, w.experience_years, w.description " +
                                "FROM workers w " +
                                "JOIN users u ON w.user_id = u.user_id " +
                                "WHERE w.service_category = ? " +
                                "AND w.verification_status = 'APPROVED' " +
                                "AND u.account_status = 'ACTIVE'";

                try (Connection conn = DatabaseConnection.getConnection();
                                PreparedStatement stmt = conn.prepareStatement(sql)) {

                        stmt.setString(1, category);

                        try (ResultSet rs = stmt.executeQuery()) {

                                while (rs.next()) {

                                        Map<String, Object> worker = new HashMap<>();

                                        int workerId = rs.getInt("worker_id");

                                        worker.put("worker_id", workerId);
                                        worker.put("name", rs.getString("name"));
                                        worker.put("phone", rs.getString("phone"));
                                        worker.put("service_category", rs.getString("service_category"));
                                        worker.put("experience_years", rs.getInt("experience_years"));
                                        worker.put("description", rs.getString("description"));

                                        List<Map<String, Object>> prices = new ArrayList<>();

                                        String priceSql = "SELECT price_id, service_name, service_price, inspection_fee "
                                                        +
                                                        "FROM worker_service_prices WHERE worker_id = ?";

                                        try (PreparedStatement priceStmt = conn.prepareStatement(priceSql)) {

                                                priceStmt.setInt(1, workerId);

                                                try (ResultSet priceRs = priceStmt.executeQuery()) {

                                                        while (priceRs.next()) {

                                                                Map<String, Object> price = new HashMap<>();

                                                                price.put("price_id", priceRs.getInt("price_id"));
                                                                price.put("service_name",
                                                                                priceRs.getString("service_name"));
                                                                price.put("service_price",
                                                                                priceRs.getBigDecimal("service_price"));
                                                                price.put("inspection_fee", priceRs
                                                                                .getBigDecimal("inspection_fee"));

                                                                prices.add(price);
                                                        }
                                                }
                                        }

                                        worker.put("prices", prices);
                                        workers.add(worker);
                                }
                        }

                } catch (SQLException e) {
                        e.printStackTrace();
                }

                return workers;
        }

}
