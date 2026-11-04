import java.util.Scanner;

public class marketplace {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        int choice = 0;

        do {
            System.out.println("\n===== ServeU =====");
            System.out.println("1. Register as Customer");
            System.out.println("2. Register as Worker");
            System.out.println("3. Login");
            System.out.println("4. Find Approved Workers");
            System.out.println("5. Exit");
            System.out.print("Enter your choice: ");

            if (!scanner.hasNextInt()) {
                System.out.println("Please enter a valid number.");
                scanner.nextLine();
                continue;
            }

            choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {

                case 1: {
                    System.out.println("\n--- Customer Registration ---");

                    System.out.print("Enter your name: ");
                    String name = scanner.nextLine();

                    System.out.print("Enter your email: ");
                    String email = scanner.nextLine();

                    System.out.print("Enter your password: ");
                    String password = scanner.nextLine();

                    System.out.print("Enter your phone: ");
                    String phone = scanner.nextLine();

                    boolean registered = userservices.registerCustomer(
                            name, email, password, phone);

                    if (registered) {
                        System.out.println("Customer registered successfully!");
                    } else {
                        System.out.println("Customer registration failed.");
                    }
                    break;
                }

                case 2: {
                    System.out.println("\n--- Worker Registration ---");

                    System.out.print("Enter your name: ");
                    String workerName = scanner.nextLine();

                    System.out.print("Enter your email: ");
                    String workerEmail = scanner.nextLine();

                    System.out.print("Enter your password: ");
                    String workerPassword = scanner.nextLine();

                    System.out.print("Enter your phone: ");
                    String workerPhone = scanner.nextLine();

                    System.out.println("Available categories:");
                    System.out.println("Plumbing, Electrical, Carpentry, Painting");
                    System.out.println("Home Cleaning, AC and Appliance Repair");
                    System.out.println("Gardening, Pest Control, Tutoring");
                    System.out.println("Computer and IT Support");

                    System.out.print("Enter your service category: ");
                    String category = scanner.nextLine();

                    System.out.print("Enter years of experience: ");

                    int experience;
                    try {
                        experience = Integer.parseInt(scanner.nextLine());

                        if (experience < 0) {
                            System.out.println(
                                    "Experience cannot be negative.");
                            break;
                        }
                    } catch (NumberFormatException e) {
                        System.out.println(
                                "Please enter a valid whole number.");
                        break;
                    }

                    System.out.print("Describe your services: ");
                    String description = scanner.nextLine();

                    boolean workerRegistered = userservices.registerWorker(
                            workerName,
                            workerEmail,
                            workerPassword,
                            workerPhone,
                            category,
                            experience,
                            description);

                    if (workerRegistered) {
                        System.out.println(
                                "Worker registered. Verification is pending.");
                    } else {
                        System.out.println("Worker registration failed.");
                    }
                    break;
                }

                case 3: {
                    System.out.println("\n--- ServeU Login ---");

                    System.out.print("Enter your email: ");
                    String loginEmail = scanner.nextLine();

                    System.out.print("Enter your password: ");
                    String loginPassword = scanner.nextLine();

                    System.out.println("Select your role:");
                    System.out.println("1. Customer");
                    System.out.println("2. Worker");
                    System.out.println("3. Admin");
                    System.out.print("Enter your choice: ");

                    String roleChoice = scanner.nextLine();
                    String selectedRole;

                    if (roleChoice.equals("1")) {
                        selectedRole = "CUSTOMER";
                    } else if (roleChoice.equals("2")) {
                        selectedRole = "WORKER";
                    } else if (roleChoice.equals("3")) {
                        selectedRole = "ADMIN";
                    } else {
                        System.out.println("Invalid role selection.");
                        break;
                    }

                    boolean loginSuccess = userservices.loginUser(
                            loginEmail, loginPassword, selectedRole);

                    if (loginSuccess) {

                        if (selectedRole.equals("ADMIN")) {
                            adminMenu(scanner);
                        } else if (selectedRole.equals("WORKER")) {
                            workerMenu(scanner);
                        } else if (selectedRole.equals("CUSTOMER")) {
                            customerMenu(scanner);
                        }

                    } else {
                        System.out.println(
                                "Login failed. Check your credentials, role, or account status.");
                    }
                    break;
                }

                case 4: {
                    System.out.println("\nAvailable categories:");
                    System.out.println(
                            "Plumbing, Electrical, Carpentry, Painting");
                    System.out.println(
                            "Home Cleaning, AC and Appliance Repair");
                    System.out.println(
                            "Gardening, Pest Control, Tutoring");
                    System.out.println("Computer and IT Support");

                    System.out.print("Enter service category: ");
                    String categoryToSearch = scanner.nextLine();

                    workerservices.findApprovedWorkers(categoryToSearch);
                    break;
                }

                case 5:
                    System.out.println("Thank you for using ServeU!");
                    break;

                default:
                    System.out.println("Invalid choice. Try again.");
            }

        } while (choice != 5);

        scanner.close();
    }

    public static void workerMenu(Scanner scanner) {

        int choice = 0;
        int workerId = workerservices.getWorkerId(
                userservices.getCurrentUserId());

        if (workerId == -1) {
            System.out.println("Worker profile not found.");
            return;
        }

        do {
            System.out.println("\n===== WORKER MENU =====");
            System.out.println("1. View Incoming Requests");
            System.out.println("2. Accept or Reject Request");
            System.out.println("3. Add Service Price");
            System.out.println("4. View My Price List");
            System.out.println("5. Mark Service as Completed");
            System.out.println("6. View Revisit Requests");
            System.out.println("7. Propose Revisit Appointment");
            System.out.println("8. Add Availability");
            System.out.println("9. View My Availability");
            System.out.println("10. Update Availability");
            System.out.println("11. Delete Availability");
            System.out.println("12. Propose Additional Parts");
            System.out.println("13. Logout");
            System.out.print("Enter choice: ");

            if (!scanner.hasNextInt()) {
                System.out.println("Enter a valid number.");
                scanner.nextLine();
                continue;
            }

            choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    Requests.viewWorkerRequests(workerId);
                    break;

                case 2:
                    Requests.updateRequestStatus(scanner, workerId);
                    break;

                case 3:
                    workerservices.addServicePrice(scanner, workerId);
                    break;

                case 4:
                    workerservices.viewMyServicePrices(workerId);
                    break;

                case 5:
                    Requests.markRequestCompleted(scanner, workerId);
                    break;

                case 6:
                    Requests.viewRevisitRequests(workerId);
                    break;

                case 7:
                    Requests.proposeRevisitAppointment(scanner, workerId);
                    break;

                case 8:
                    workerservices.addAvailability(scanner, workerId);
                    break;

                case 9:
                    workerservices.viewAvailability(workerId);
                    break;

                case 10:
                    workerservices.updateAvailability(scanner, workerId);
                    break;

                case 11:
                    workerservices.deleteAvailability(scanner, workerId);
                    break;
                case 12:
                    Requests.proposeAdditionalParts(scanner, workerId);
                    break;

                case 13:
                    System.out.println("Worker logged out.");
                    break;

                default:
                    System.out.println("Invalid choice.");
            }

        } while (choice != 13);
    }

    private static void adminMenu(Scanner scanner) {

        int adminChoice = 0;

        do {
            System.out.println("1. View Pending Workers");
            System.out.println("2. Approve or Reject a Worker");
            System.out.println("3. View Customer Reports");
            System.out.println("4. Update Report Status");
            System.out.println("5. Logout");
            System.out.print("Enter your choice: ");

            if (!scanner.hasNextInt()) {
                System.out.println("Please enter a valid number.");
                scanner.nextLine();
                continue;
            }

            adminChoice = scanner.nextInt();
            scanner.nextLine();

            switch (adminChoice) {

                case 1:
                    Verification.listPendingWorkers();
                    break;

                case 2: {
                    System.out.print("Enter worker ID: ");

                    int workerId;
                    try {
                        workerId = Integer.parseInt(scanner.nextLine());

                        if (workerId <= 0) {
                            System.out.println("Invalid worker ID.");
                            break;
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Enter a valid worker ID.");
                        break;
                    }

                    String status = Verification.getWorkerVerificationStatus(workerId);

                    if (!"PENDING".equals(status)) {
                        System.out.println(
                                "Worker not found or is not pending. Status: "
                                        + status);
                        break;
                    }

                    System.out.println("1. Approve worker");
                    System.out.println("2. Reject worker");
                    System.out.print("Enter your decision: ");

                    String decisionChoice = scanner.nextLine();
                    String decision;

                    if (decisionChoice.equals("1")) {
                        decision = "APPROVED";
                    } else if (decisionChoice.equals("2")) {
                        decision = "REJECTED";
                    } else {
                        System.out.println("Invalid decision.");
                        break;
                    }

                    System.out.print("Enter admin remarks: ");
                    String remarks = scanner.nextLine();

                    Verification.updateWorkerVerification(
                            workerId, decision, remarks);
                    break;
                }

                case 3:
                    Verification.listCustomerReports();
                    break;

                case 4: {
                    System.out.print("Enter Report ID: ");

                    int reportId;

                    try {
                        reportId = Integer.parseInt(scanner.nextLine());

                        if (reportId <= 0) {
                            System.out.println("Invalid Report ID.");
                            break;
                        }

                    } catch (NumberFormatException e) {
                        System.out.println("Enter a valid Report ID.");
                        break;
                    }

                    System.out.println("\nSelect report status:");
                    System.out.println("1. UNDER_REVIEW");
                    System.out.println("2. RESOLVED");
                    System.out.println("3. DISMISSED");
                    System.out.print("Enter your choice: ");

                    String statusChoice = scanner.nextLine();
                    String decision = null;

                    switch (statusChoice) {
                        case "1":
                            decision = "UNDER_REVIEW";
                            break;
                        case "2":
                            decision = "RESOLVED";
                            break;
                        case "3":
                            decision = "DISMISSED";
                            break;
                        default:
                            System.out.println("Invalid status choice.");
                            break;
                    }

                    if (decision == null) {
                        break;
                    }

                    System.out.print("Enter admin remarks: ");
                    String remarks = scanner.nextLine();

                    Verification.updateReportStatus(
                            reportId, decision, remarks);

                    break;
                }
                case 5:
                    System.out.println("Admin logged out.");
                    break;
                default:
                    System.out.println("Invalid choice. Try again.");
            }

        } while (adminChoice != 5);
    }

    public static void customerMenu(Scanner scanner) {

        int choice = 0;
        int customerId = userservices.getCurrentUserId();

        do {
            System.out.println("1. Find Approved Workers");
            System.out.println("2. Create Service Request");
            System.out.println("3. View My Requests");
            System.out.println("4. Confirm Service Completion");
            System.out.println("5. View Proposed Revisit Appointments");
            System.out.println("6. Respond to Revisit Proposal");
            System.out.println("7. Submit Service Review");
            System.out.println("8. Submit a Report");
            System.out.println("9. View My Reports");
            System.out.println("10. Approve or Reject Additional Parts");
            System.out.println("11. Logout");
            System.out.print("Enter your choice: ");
            if (!scanner.hasNextInt()) {
                System.out.println("Please enter a valid number.");
                scanner.nextLine();
                continue;
            }

            choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {

                case 1:
                    System.out.print("Enter service category: ");
                    String category = scanner.nextLine();

                    workerservices.findApprovedWorkers(category);
                    break;

                case 2:
                    System.out.print("Enter service category: ");
                    String requestCategory = scanner.nextLine();

                    workerservices.findApprovedWorkers(requestCategory);

                    System.out.print("\nEnter Worker ID: ");

                    int workerId;

                    try {
                        workerId = Integer.parseInt(scanner.nextLine());

                        if (workerId <= 0) {
                            System.out.println("Invalid Worker ID.");
                            break;
                        }

                    } catch (NumberFormatException e) {
                        System.out.println("Please enter a valid Worker ID.");
                        break;
                    }

                    Requests.createRequest(scanner, customerId, workerId);
                    break;

                case 3:
                    Requests.viewCustomerRequests(customerId);
                    break;

                case 4:
                    Requests.confirmServiceCompletion(scanner, customerId);
                    break;

                case 5:
                    Requests.viewProposedRevisits(customerId);
                    break;

                case 6:
                    Requests.respondToRevisitProposal(scanner, customerId);
                    break;

                case 7:
                    Requests.submitReview(scanner, customerId);
                    break;

                case 8:
                    Requests.submitReport(scanner, customerId);
                    break;

                case 9:
                    Requests.viewMyReports(customerId);
                    break;

                case 10:
                    Requests.respondToPartsProposal(scanner, customerId);
                    break;

                case 11:
                    System.out.println("Logging out...");
                    break;

                default:
                    System.out.println("Invalid choice.");
            }

        } while (choice != 11);
    }

}