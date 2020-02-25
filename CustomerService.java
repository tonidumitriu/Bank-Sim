public class CustomerService {
    int customerId;
    double arrivalTime;
    double serviceTime;
    int queueLoc;
    int tellerLoc;
    double timeServiceBegins;
    double timeCustomerWaits;
    double timeServiceEnds;
    double timeSpentInSystem;

    public CustomerService(int customerId, double arrivalTime, double serviceTime,
            int queueLoc, int tellerLoc, double timeServiceBegins, double timeCustomerWaits,
            double timeServiceEnds, double timeSpentInSystem) {
        this.customerId = customerId;
        this.arrivalTime = arrivalTime;
        this.serviceTime = serviceTime;
        this.queueLoc = queueLoc;
        this.tellerLoc = tellerLoc;
        this.timeServiceBegins = timeServiceBegins;
        this.timeCustomerWaits = timeCustomerWaits;
        this.timeServiceEnds = timeServiceEnds;
        this.timeSpentInSystem = timeSpentInSystem;
    }
}
