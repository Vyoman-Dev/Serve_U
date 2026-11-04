public class TestVerification {
    public static void main(String[] args) {
        String status = Verification.getWorkerVerificationStatus(1);

        System.out.println("Worker verification status: " + status);

        Verification.listPendingWorkers();
    }
}