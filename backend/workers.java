public class workers {
    private int workerId;
    private int userId;
    private String serviceCategory;
    private int experienceYears;
    private String description;
    private String verificationStatus;

    public workers
    (int workerId, int userId, String serviceCategory,int experienceYears, String description,String verificationStatus) {
        this.workerId = workerId;
        this.userId = userId;
        this.serviceCategory = serviceCategory;
        this.experienceYears = experienceYears;
        this.description = description;
        this.verificationStatus = verificationStatus;
    }
}
