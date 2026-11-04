import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.math.BigDecimal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PaymentServices {
    public static boolean submitPayment(
            int customerId,
            int requestId,
            String paymentMethod,
            String transactionReference) {

        String sql = "SELECT worker_id, service_price, inspection_fee, " +
                "parts_cost, parts_status " +
                "FROM service_requests " +
                "WHERE request_id = ? AND customer_id = ? " +
                "AND request_status = 'COMPLETED'";

        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, requestId);
            ps.setInt(2, customerId);

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next()) {
                    System.out.println("Completed service request not found.");
                    return false;
                }

                int workerId = rs.getInt("worker_id");

                BigDecimal serviceAmount = rs.getBigDecimal("service_price");

                BigDecimal inspectionFee = rs.getBigDecimal("inspection_fee");

                String partsStatus = rs.getString("parts_status");

                BigDecimal partsAmount = "APPROVED".equals(partsStatus)
                        ? rs.getBigDecimal("parts_cost")
                        : BigDecimal.ZERO;

                if (serviceAmount == null) {
                    System.out.println("Service price is missing.");
                    return false;
                }

                if (inspectionFee == null) {
                    inspectionFee = BigDecimal.ZERO;
                }

                if (partsAmount == null) {
                    partsAmount = BigDecimal.ZERO;
                }

                BigDecimal totalAmount = serviceAmount
                        .add(inspectionFee)
                        .add(partsAmount);

                if (!"UPI".equals(paymentMethod)
                        && !"CASH".equals(paymentMethod)
                        && !"BANK_TRANSFER".equals(paymentMethod)) {
                    System.out.println("Invalid payment method.");
                    return false;
                }

                String insertSql = "INSERT INTO payments " +
                        "(request_id, customer_id, worker_id, " +
                        "service_amount, inspection_fee, parts_amount, " +
                        "total_amount, payment_method, transaction_reference, " +
                        "payment_status, customer_confirmed_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, " +
                        "'CUSTOMER_SUBMITTED', CURRENT_TIMESTAMP)";

                try (PreparedStatement insertPs = con.prepareStatement(insertSql)) {

                    insertPs.setInt(1, requestId);
                    insertPs.setInt(2, customerId);
                    insertPs.setInt(3, workerId);
                    insertPs.setBigDecimal(4, serviceAmount);
                    insertPs.setBigDecimal(5, inspectionFee);
                    insertPs.setBigDecimal(6, partsAmount);
                    insertPs.setBigDecimal(7, totalAmount);
                    insertPs.setString(8, paymentMethod);
                    insertPs.setString(9, transactionReference);

                    int rows = insertPs.executeUpdate();

                    if (rows > 0) {
                        System.out.println("Payment details submitted.");
                        System.out.println("Total amount: ₹" + totalAmount);
                        return true;
                    }
                }

            }

        } catch (SQLException e) {
            System.out.println("Payment submission failed:");
            e.printStackTrace();
        }
        return false;
    }

    public static boolean confirmPayment(int workerId, int paymentId) {

        String selectSql = "SELECT total_amount FROM payments " +
                "WHERE payment_id = ? AND worker_id = ? " +
                "AND payment_status = 'CUSTOMER_SUBMITTED'";

        String updateSql = "UPDATE payments SET payment_status = 'WORKER_CONFIRMED', " +
                "worker_confirmed_at = CURRENT_TIMESTAMP " +
                "WHERE payment_id = ? AND worker_id = ? " +
                "AND payment_status = 'CUSTOMER_SUBMITTED'";

        String insertSql = "INSERT INTO worker_commissions " +
                "(payment_id, worker_id, commission_rate, commission_amount) " +
                "VALUES (?, ?, 5.00, ?)";

        try (Connection con = DatabaseConnection.getConnection()) {

            con.setAutoCommit(false);

            try {
                BigDecimal totalAmount;

                // Step 1: Find the payment
                try (PreparedStatement ps = con.prepareStatement(selectSql)) {

                    ps.setInt(1, paymentId);
                    ps.setInt(2, workerId);

                    try (ResultSet rs = ps.executeQuery()) {

                        if (!rs.next()) {
                            con.rollback();
                            System.out.println("Payment not found or already confirmed.");
                            return false;
                        }

                        totalAmount = rs.getBigDecimal("total_amount");
                    }
                }

                // Step 2: Calculate 5% commission
                BigDecimal commission = totalAmount
                        .multiply(new BigDecimal("0.05"))
                        .setScale(2, java.math.RoundingMode.HALF_UP);

                // Step 3: Update payment status
                try (PreparedStatement ps = con.prepareStatement(updateSql)) {

                    ps.setInt(1, paymentId);
                    ps.setInt(2, workerId);

                    if (ps.executeUpdate() == 0) {
                        con.rollback();
                        return false;
                    }
                }

                // Step 4: Save commission
                try (PreparedStatement ps = con.prepareStatement(insertSql)) {

                    ps.setInt(1, paymentId);
                    ps.setInt(2, workerId);
                    ps.setBigDecimal(3, commission);

                    ps.executeUpdate();
                }

                con.commit();

                System.out.println("Payment confirmed successfully.");
                System.out.println("Total payment: ₹" + totalAmount);
                System.out.println("5% commission: ₹" + commission);

                return true;

            } catch (SQLException e) {
                con.rollback();
                throw e;
            }

        } catch (SQLException e) {
            System.out.println("Payment confirmation failed: " + e.getMessage());
            return false;
        }
    }

    public static List<Map<String, Object>> getPayments(
            int userId, String role) throws SQLException {

        List<Map<String, Object>> payments = new ArrayList<>();

        String sql;

        if ("CUSTOMER".equals(role)) {

            sql = "SELECT p.* FROM payments p " +
                    "WHERE p.customer_id = ? " +
                    "ORDER BY p.created_at DESC";

        } else if ("WORKER".equals(role)) {

            sql = "SELECT p.* FROM payments p " +
                    "JOIN workers w ON p.worker_id = w.worker_id " +
                    "WHERE w.user_id = ? " +
                    "ORDER BY p.created_at DESC";

        } else {
            throw new IllegalArgumentException("Invalid role.");
        }

        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    Map<String, Object> payment = new HashMap<>();

                    payment.put("payment_id", rs.getInt("payment_id"));
                    payment.put("request_id", rs.getInt("request_id"));
                    payment.put("total_amount", rs.getBigDecimal("total_amount"));
                    payment.put("payment_method", rs.getString("payment_method"));
                    payment.put("payment_status", rs.getString("payment_status"));
                    payment.put("created_at", rs.getTimestamp("created_at"));

                    payments.add(payment);
                }
            }
        }

        return payments;
    }
}