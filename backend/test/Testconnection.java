import java.sql.Connection;

public class Testconnection {
    public static void main(String args[]) {
        try (Connection connection = DatabaseConnection.getConnection()) 
        { if (connection != null) {
                System.out.println("Connected to MySQL successfully!");
            }
        } 
        catch (Exception e) {
            System.out.println("Connection failed!");
            e.printStackTrace();
        }
    }
}
