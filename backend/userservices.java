
import java.sql.*;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

public class userservices {
    private static int currentUserId = -1;
    private static String currentUserRole = null;

    public static int getCurrentUserId() {
        return currentUserId;
    }

    public static String getCurrentUserRole() {
        return currentUserRole;
    }

    public static boolean loginUser(
            String email,
            String password,
            String selectedRole) {
        currentUserId = -1;
        currentUserRole = null;

        if (email == null || email.trim().isEmpty()
                || password == null || password.isEmpty()
                || selectedRole == null) {
            System.out.println("Enter all login details.");
            return false;
        }

        selectedRole = selectedRole.trim().toUpperCase();

        if (!selectedRole.equals("CUSTOMER")
                && !selectedRole.equals("WORKER")
                && !selectedRole.equals("ADMIN")) {
            System.out.println("Invalid role selected.");
            return false;
        }

        String sql = "SELECT user_id, name, password_hash, account_status, role " +
                "FROM users WHERE email = ? AND role = ?";

        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, email.trim());
            stmt.setString(2, selectedRole);

            try (ResultSet rs = stmt.executeQuery()) {

                if (!rs.next()) {
                    System.out.println("Invalid email, password, or role.");
                    return false;
                }

                String storedHash = rs.getString("password_hash");

                if (!S_Password.verifyPassword(password, storedHash)) {
                    System.out.println("Invalid email, password, or role.");
                    return false;
                }

                String status = rs.getString("account_status");

                if (!"ACTIVE".equals(status)) {
                    System.out.println("Your account is inactive.");
                    return false;
                }

                currentUserId = rs.getInt("user_id");
                currentUserRole = rs.getString("role");

                System.out.println("Login successful!");
                System.out.println("Welcome, " + rs.getString("name") + "!");
                System.out.println("Role: " + currentUserRole);
                System.out.println("User ID: " + currentUserId);

                return true;
            }

        } catch (SQLException | GeneralSecurityException e) {
            System.out.println("Login failed due to a system error.");
            e.printStackTrace();
            return false;
        }
    }

    public static boolean registerWorker(
            String name,
            String email,
            String password,
            String phone,
            String serviceCategory,
            int experienceYears,
            String description) {

        if (name == null || name.trim().isEmpty()
                || email == null || email.trim().isEmpty()
                || password == null || password.length() < 8
                || serviceCategory == null || serviceCategory.trim().isEmpty()
                || experienceYears < 0) {

            System.out.println("Invalid worker registration details.");
            return false;
        }

        String passwordHash;

        try {
            passwordHash = S_Password.hashPassword(password);
        } catch (GeneralSecurityException e) {
            System.out.println("Password hashing failed.");
            return false;
        }

        String checkSql = "SELECT user_id FROM users WHERE email = ?";

        String userSql = "INSERT INTO users (name, email, password_hash, phone, role) " +
                "VALUES (?, ?, ?, ?, 'WORKER')";

        String workerSql = "INSERT INTO workers " +
                "(user_id, service_category, experience_years, description) " +
                "VALUES (?, ?, ?, ?)";

        String verificationSql = "INSERT INTO verifications " +
                "(worker_id, document_description, verification_status) " +
                "VALUES (?, ?, 'PENDING')";

        try (Connection con = DatabaseConnection.getConnection()) {

            try (PreparedStatement check = con.prepareStatement(checkSql)) {
                check.setString(1, email.trim());

                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("Email is already registered!");
                        return false;
                    }
                }
            }

            con.setAutoCommit(false);

            try {
                int userId;
                int workerId;

                // Insert user account
                try (PreparedStatement insertUser = con.prepareStatement(
                        userSql, java.sql.Statement.RETURN_GENERATED_KEYS)) {

                    insertUser.setString(1, name.trim());
                    insertUser.setString(2, email.trim());
                    insertUser.setString(3, passwordHash);
                    insertUser.setString(4, phone);

                    insertUser.executeUpdate();

                    try (ResultSet keys = insertUser.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Could not retrieve user ID.");
                        }
                        userId = keys.getInt(1);
                    }
                }

                // Insert worker profile and retrieve worker ID
                try (PreparedStatement insertWorker = con.prepareStatement(
                        workerSql, java.sql.Statement.RETURN_GENERATED_KEYS)) {

                    insertWorker.setInt(1, userId);
                    insertWorker.setString(2, serviceCategory.trim());
                    insertWorker.setInt(3, experienceYears);
                    insertWorker.setString(4, description);

                    insertWorker.executeUpdate();

                    try (ResultSet keys = insertWorker.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Could not retrieve worker ID.");
                        }
                        workerId = keys.getInt(1);
                    }
                }

                // Insert verification record
                try (PreparedStatement insertVerification = con.prepareStatement(verificationSql)) {

                    insertVerification.setInt(1, workerId);
                    insertVerification.setString(
                            2,
                            description == null || description.trim().isEmpty()
                                    ? "No document description provided"
                                    : description.trim());

                    insertVerification.executeUpdate();
                }

                con.commit();

                System.out.println("Worker registered successfully!");
                System.out.println("User ID: " + userId);
                System.out.println("Worker ID: " + workerId);
                System.out.println("Verification status: PENDING");

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
                    System.err.println("Warning: Could not reset transaction settings.");
                }
            }

        } catch (SQLException e) {
            System.out.println("Worker registration failed.");
            e.printStackTrace();
            return false;
        }
    }

    public static boolean loginCustomer(String email, String password) {

        if (email == null || email.trim().isEmpty()
                || password == null || password.isEmpty()) {
            System.out.println("Enter your email and password.");
            return false;
        }

        String sql = "SELECT user_id, name, password_hash, account_status " +
                "FROM users WHERE email = ? AND role = 'CUSTOMER'";

        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, email.trim());

            try (ResultSet rs = stmt.executeQuery()) {

                if (!rs.next()) {
                    System.out.println("Invalid email or password.");
                    return false;
                }

                String storedHash = rs.getString("password_hash");

                if (!S_Password.verifyPassword(password, storedHash)) {
                    System.out.println("Invalid email or password.");
                    return false;
                }

                String status = rs.getString("account_status");

                if (!"ACTIVE".equals(status)) {
                    System.out.println("Your account is inactive.");
                    return false;
                }

                System.out.println("Login successful!");
                System.out.println("Welcome, " + rs.getString("name") + "!");
                System.out.println("Customer ID: " + rs.getInt("user_id"));

                return true;
            }

        } catch (SQLException | GeneralSecurityException e) {
            System.out.println("Login failed due to a system error.");
            e.printStackTrace();
            return false;
        }
    }

    public static boolean registerCustomer(
            String name,
            String email,
            String password,
            String phone) {

        // Validate the input
        if (name == null || name.trim().isEmpty()
                || email == null || email.trim().isEmpty()
                || password == null || password.length() < 8) {

            System.out.println(
                    "Enter a name, email, and password of at least 8 characters.");

            return false;
        }

        // Hash the password
        String passwordHash;

        try {
            passwordHash = S_Password.hashPassword(password);

        } catch (GeneralSecurityException e) {
            System.out.println("Password hashing failed.");
            e.printStackTrace();
            return false;
        }

        String checkSql = "SELECT user_id FROM users WHERE email = ?";

        String insertSql = "INSERT INTO users " +
                "(name, email, password_hash, phone, role) " +
                "VALUES (?, ?, ?, ?, 'CUSTOMER')";

        try (Connection con = DatabaseConnection.getConnection()) {

            // Check whether the email already exists
            try (PreparedStatement check = con.prepareStatement(checkSql)) {

                check.setString(1, email.trim());

                try (ResultSet rs = check.executeQuery()) {

                    if (rs.next()) {
                        System.out.println(
                                "Email is already registered!");
                        return false;
                    }
                }
            }

            // Insert the customer into the database
            try (PreparedStatement insert = con.prepareStatement(insertSql)) {

                insert.setString(1, name.trim());
                insert.setString(2, email.trim());
                insert.setString(3, passwordHash);
                insert.setString(4, phone);

                int rowsInserted = insert.executeUpdate();

                if (rowsInserted == 1) {
                    System.out.println(
                            "Customer registered successfully!");
                    return true;
                }
            }

        } catch (SQLException e) {
            System.out.println("Registration failed.");
            e.printStackTrace();
        }

        return false;
    }

    public static Map<String, Object> authenticateApiUser(
            String email,
            String password,
            String selectedRole) {

        if (email == null || email.trim().isEmpty()
                || password == null || password.isEmpty()
                || selectedRole == null) {
            return null;
        }

        selectedRole = selectedRole.trim().toUpperCase();

        if (!selectedRole.equals("CUSTOMER")
                && !selectedRole.equals("WORKER")
                && !selectedRole.equals("ADMIN")) {
            return null;
        }

        String sql = "SELECT user_id, name, password_hash, account_status, role "
                + "FROM users WHERE email = ? AND role = ?";

        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, email.trim());
            stmt.setString(2, selectedRole);

            try (ResultSet rs = stmt.executeQuery()) {

                if (!rs.next()) {
                    return null;
                }

                String storedHash = rs.getString("password_hash");

                if (!S_Password.verifyPassword(password, storedHash)) {
                    return null;
                }

                if (!"ACTIVE".equals(rs.getString("account_status"))) {
                    return null;
                }

                Map<String, Object> user = new HashMap<>();

                user.put("user_id", rs.getInt("user_id"));
                user.put("name", rs.getString("name"));
                user.put("role", rs.getString("role"));

                return user;
            }

        } catch (SQLException | GeneralSecurityException e) {
            System.out.println("API authentication error: " + e.getMessage());
            return null;
        }
    }
}
