
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TestLogin {
    public static void main(String[] args) {
        String email = "admin@serveu.com";
        String newPassword = "Admin_Pass12345";

        try {
            String newHash = S_Password.hashPassword(newPassword);

            try (Connection con = DatabaseConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(
                     "UPDATE users SET password_hash = ? " +
                     "WHERE email = ? AND role = 'ADMIN'")) {

                ps.setString(1, newHash);
                ps.setString(2, email);

                int rows = ps.executeUpdate();

                if (rows == 1) {
                    System.out.println("Admin password reset successfully.");

                    boolean matches =
                        S_Password.verifyPassword(newPassword, newHash);

                    System.out.println("Password verification: " + matches);
                } else {
                    System.out.println("Admin account not found.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
