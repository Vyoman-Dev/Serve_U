
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Verification {

    public static String getWorkerVerificationStatus(int workerId) {
        String sql = "SELECT verification_status FROM workers WHERE worker_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, workerId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("verification_status");
                } else {
                    return "WORKER_NOT_FOUND";
                }
            }

        } catch (SQLException e) {
            System.out.println("Error checking verification status: " + e.getMessage());
            return "ERROR";
        }
    }

    public static void listPendingWorkers() {
        String sql = "SELECT w.worker_id, u.name, u.email, u.phone, " +
                "w.service_category, w.experience_years, w.description " +
                "FROM workers w " +
                "JOIN users u ON w.user_id = u.user_id " +
                "WHERE w.verification_status = 'PENDING' " +
                "AND u.account_status = 'ACTIVE' " +
                "ORDER BY w.worker_id";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            boolean found = false;

            while (rs.next()) {
                found = true;

                System.out.println("\nWorker ID: " + rs.getInt("worker_id"));
                System.out.println("Name: " + rs.getString("name"));
                System.out.println("Email: " + rs.getString("email"));
                System.out.println("Phone: " + rs.getString("phone"));
                System.out.println("Category: " + rs.getString("service_category"));
                System.out.println("Experience: " + rs.getInt("experience_years") + " years");
                System.out.println("Description: " + rs.getString("description"));
                System.out.println("Verification Status: PENDING");
                System.out.println("------------------------------");
            }

            if (!found) {
                System.out.println("No workers are waiting for verification.");
            }

        } catch (SQLException e) {
            System.out.println("Error listing pending workers: " + e.getMessage());
        }
    }

    public static boolean updateWorkerVerification(
            int workerId, String decision, String adminRemarks) {

        if (workerId <= 0 || decision == null) {
            System.out.println("Invalid verification details.");
            return false;
        }

        decision = decision.trim().toUpperCase();

        if (!decision.equals("APPROVED") && !decision.equals("REJECTED")) {
            System.out.println("Decision must be APPROVED or REJECTED.");
            return false;
        }

        String workerSql = "UPDATE workers SET verification_status = ? " +
                "WHERE worker_id = ? AND verification_status = 'PENDING'";

        String verificationSql = "UPDATE verifications SET verification_status = ?, admin_remarks = ? " +
                "WHERE worker_id = ? AND verification_status = 'PENDING'";

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                int workerRows;

                try (PreparedStatement ps = con.prepareStatement(workerSql)) {
                    ps.setString(1, decision);
                    ps.setInt(2, workerId);
                    workerRows = ps.executeUpdate();
                }

                if (workerRows != 1) {
                    con.rollback();
                    System.out.println(
                            "Worker not found or is no longer pending.");
                    return false;
                }

                int verificationRows;

                try (PreparedStatement ps = con.prepareStatement(verificationSql)) {
                    ps.setString(1, decision);
                    ps.setString(2, adminRemarks);
                    ps.setInt(3, workerId);
                    verificationRows = ps.executeUpdate();
                }

                if (verificationRows != 1) {
                    con.rollback();
                    System.out.println(
                            "Verification record missing or no longer pending. Changes rolled back.");
                    return false;
                }

                con.commit();
                System.out.println(
                        "Worker verification updated to " + decision + ".");
                return true;

            } catch (SQLException e) {
                try {
                    con.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            } finally {
                try {
                    con.setAutoCommit(true);
                } catch (SQLException resetError) {
                    System.err.println(
                            "Warning: Could not reset transaction settings.");
                }
            }

        } catch (SQLException e) {
            System.out.println("Could not update worker verification.");
            e.printStackTrace();
            return false;
        }
    }

    public static void listCustomerReports() {

        String sql = "SELECT r.report_id, r.reporter_id, "
                + "reporter.name AS reporter_name, "
                + "r.reported_user_id, reported.name AS reported_name, "
                + "r.request_id, r.report_reason, "
                + "r.report_description, r.report_status, "
                + "r.admin_remarks, r.created_at "
                + "FROM reports r "
                + "JOIN users reporter ON r.reporter_id = reporter.user_id "
                + "JOIN users reported ON r.reported_user_id = reported.user_id "
                + "ORDER BY r.created_at DESC";

        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            boolean found = false;

            while (rs.next()) {
                found = true;

                System.out.println("\n========== CUSTOMER REPORT ==========");
                System.out.println("Report ID: " + rs.getInt("report_id"));
                System.out.println("Reporter: " + rs.getString("reporter_name")
                        + " (User ID: " + rs.getInt("reporter_id") + ")");
                System.out.println("Reported Worker: " + rs.getString("reported_name")
                        + " (User ID: " + rs.getInt("reported_user_id") + ")");
                System.out.println("Request ID: " + rs.getInt("request_id"));
                System.out.println("Reason: " + rs.getString("report_reason"));
                System.out.println("Description: " + rs.getString("report_description"));
                System.out.println("Status: " + rs.getString("report_status"));
                System.out.println("Admin Remarks: " + rs.getString("admin_remarks"));
                System.out.println("Submitted At: " + rs.getTimestamp("created_at"));
                System.out.println("=====================================");
            }

            if (!found) {
                System.out.println("No customer reports found.");
            }

        } catch (SQLException e) {
            System.out.println("Error listing customer reports: " + e.getMessage());
        }
    }

    
public static boolean updateReportStatus(
        int reportId, String decision, String adminRemarks) {

    if (reportId <= 0) {
        System.out.println("Invalid Report ID.");
        return false;
    }

    if (decision == null) {
        System.out.println("Invalid report status.");
        return false;
    }

    decision = decision.trim().toUpperCase();

    if (!decision.equals("UNDER_REVIEW")
            && !decision.equals("RESOLVED")
            && !decision.equals("DISMISSED")) {

        System.out.println("Invalid report status.");
        return false;
    }

    if (adminRemarks == null || adminRemarks.trim().isEmpty()
            || adminRemarks.trim().length() > 500) {
        System.out.println("Remarks must contain 1-500 characters.");
        return false;
    }

    String sql = "UPDATE reports "
            + "SET report_status = ?, admin_remarks = ? "
            + "WHERE report_id = ? "
            + "AND report_status IN ('PENDING', 'UNDER_REVIEW')";

    try (Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setString(1, decision);
        ps.setString(2, adminRemarks.trim());
        ps.setInt(3, reportId);

        int rowsUpdated = ps.executeUpdate();

        if (rowsUpdated == 1) {
            System.out.println(
                    "Report successfully updated to " + decision + ".");
            return true;
        } else {
            System.out.println(
                    "Report not found or has already been processed.");
            return false;
        }

    } catch (SQLException e) {
        System.out.println("Could not update report status.");
        e.printStackTrace();
        return false;
    }
}


    public static List<Map<String, Object>> getPendingWorkers() {

        List<Map<String, Object>> workers = new ArrayList<>();

        String sql = "SELECT w.worker_id, u.name, u.email, u.phone, " +
                "w.service_category, w.experience_years, w.description " +
                "FROM workers w " +
                "JOIN users u ON w.user_id = u.user_id " +
                "WHERE w.verification_status = 'PENDING' " +
                "AND u.account_status = 'ACTIVE' " +
                "ORDER BY w.worker_id";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {

                Map<String, Object> worker = new HashMap<>();

                worker.put("worker_id", rs.getInt("worker_id"));
                worker.put("name", rs.getString("name"));
                worker.put("email", rs.getString("email"));
                worker.put("phone", rs.getString("phone"));
                worker.put("service_category", rs.getString("service_category"));
                worker.put("experience_years", rs.getInt("experience_years"));
                worker.put("description", rs.getString("description"));
                worker.put("verification_status", "PENDING");

                workers.add(worker);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return workers;
    }

}
