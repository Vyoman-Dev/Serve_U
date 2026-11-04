
public class servicerequest {
    private int requestId;
    private int customerId;
    private int workerId;
    private String serviceDescription;
    private String requestStatus;

    public servicerequest(int requestId, int customerId, int workerId,
                          String serviceDescription, String requestStatus) {
        this.requestId = requestId;
        this.customerId = customerId;
        this.workerId = workerId;
        this.serviceDescription = serviceDescription;
        this.requestStatus = requestStatus;
    }

    public int getRequestId() {
        return requestId;
    }

    public int getCustomerId() {
        return customerId;
    }

    public int getWorkerId() {
        return workerId;
    }

    public String getServiceDescription() {
        return serviceDescription;
    }

    public String getRequestStatus() {
        return requestStatus;
    }
}
