public class ResetCustomerPassword {

    public static void main(String[] args) {

        String newPassword = "ServeU_Customer@2026";

        try {
            String passwordHash =
                    S_Password.hashPassword(newPassword);

            System.out.println("Password hash:");
            System.out.println(passwordHash);

        } catch (Exception e) {
            System.out.println("Error generating password hash.");
            e.printStackTrace();
        }
    }
}