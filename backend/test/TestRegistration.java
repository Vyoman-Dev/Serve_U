
public class TestRegistration {

    public static void main(String[] args) {

        boolean result = userservices.registerCustomer(
                "Test Customer",
                "test.customer@example.com",
                "TestPass123!",
                "9876543210"
        );

        System.out.println("Registration result: " + result);
    }
}
