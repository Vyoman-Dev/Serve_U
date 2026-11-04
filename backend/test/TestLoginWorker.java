
public class TestLoginWorker {
    public static void main(String[] args) {

        // Test 1: Correct worker login
        boolean result = userservices.loginUser(
                "test.worker@example.com",
                "TestWorker123!",
                "WORKER"
        );

        System.out.println("Worker login result: " + result);

        // Test 2: Try logging in as a customer
        boolean wrongRole = userservices.loginUser(
                "test.worker@example.com",
                "TestWorker123!",
                "CUSTOMER"
        );

        System.out.println("Wrong role login result: " + wrongRole);
    }
}
