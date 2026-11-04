
public class TestRegistrationWorker {
    public static void main(String[] args) {
        boolean result = userservices.registerWorker(
                "Test Worker",
                "test.worker@example.com",
                "TestWorker123!",
                "9876543211",
                "Plumbing",
                3,
                "Test worker profile for registration testing"
        );

        System.out.println("Worker registration result: " + result);
    }
}
