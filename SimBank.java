import java.util.ArrayList;
import java.util.LinkedList;
import java.util.PriorityQueue;

class SimBank {
    // data members of the class

    int n_tell;                               // number of tellers
    boolean queue;                            // whether or not to use a single queue
    double hrs;                               // hours the simulation will run
    double arr_rate;                          // rate of customer arrivals per hour
    double t_min;                             // average transaction time in minutes
    int maxq;                                 // maximum number of customers waiting (not being serviced)
    long seed;                                // random seed to initialize RandDist object

    private ArrayList<Teller> tellers;                // The list of tellers
    private ArrayList<LinkedList<Customer>> queues;   // The list of queues

    private ArrayList<Customer> processedCustomerList = new ArrayList<>();
    private ArrayList<Customer> unProcessedCustomerList = new ArrayList<>();

    // Keep track of how many arrivals, completions, and turned aways have occurred.
    private int customerArrivals = 0;
    private int customersServed = 0;
    private int numberTurnedAway = 0;

    // initializing the data members with the values of the passed arguments
    SimBank(int n_tell, boolean queue, double hrs, double arr_rate, double t_min, int maxq, long seed) {
        // creating an instance of the variables in the constructor
        this.n_tell = n_tell;
        this.hrs = hrs;
        this.arr_rate = arr_rate;
        this.t_min = t_min;
        this.maxq = maxq;
        this.queue = queue;
        this.seed = seed;

        // initializing some bank data fields

        // Initialize the list of tellers
        tellers = new ArrayList<Teller>();
        // Create the tellers and add them in the list
        for (int i = 0; i < n_tell; i++) {
            Teller teller = new Teller(i);
            tellers.add(teller);
        }

        // Initialize the list of queues (that are in front of the tellers)
        queues = new ArrayList<LinkedList<Customer>>();
        // Create the queues and add them in the list
        if(queue) {
            LinkedList<Customer> tellers_unique_queue = new LinkedList<Customer>();
            queues.add(tellers_unique_queue);
        } else {
            for (int i = 0; i < n_tell; i++) {
                LinkedList<Customer> teller_queue = new LinkedList<Customer>();
                queues.add(teller_queue);
            }
        }
    }

    public void runSimulation() {
        // The events will be stored in a priority queue, prioritized by the timestamp of the events.
        PriorityQueue<SimEvent> futureEventList = new PriorityQueue<SimEvent>();

        // Initializing the start time and the end time of banking operation in minutes
        double current_time = 0.0;
        double total_simulation_time = hrs * 60;
        double stop_time = current_time + total_simulation_time;

        // Create the initial arrival event and load it in the future event list
        double customer_arrival_rate = arr_rate / 60;  // get arrival rate per minute
        double customer_service_rate = 1 / t_min;      // get service rate in numbers per minute
        RandDist R = new RandDist(seed);
        double next_arr_min = R.exponential(customer_arrival_rate);
        ArrivalEvent first_arrival = new ArrivalEvent(next_arr_min);
        futureEventList.offer(first_arrival);

        int customer_id = 0;

        // Keep processing as long as there are still events remaining to be processed.
        while (futureEventList.size() > 0) {
            // Get the next event from the future event list
            SimEvent currentEvent = futureEventList.poll();
            current_time = currentEvent.get_e_time();


            // Check to see the type of event using instanceof operator
            if (currentEvent instanceof ArrivalEvent) {
                // Update the customer id counter and the arrivals counter
                customer_id++;
                customerArrivals++;
                // Generate a new Customer
                Customer newCustomer = new Customer(customer_id, currentEvent.get_e_time());
                // Generate a service time for the new customer
                double service_time = R.exponential(customer_service_rate);

                newCustomer.setServiceT(service_time);
                newCustomer.setArrivalT(currentEvent.get_e_time());

                //
                // Process the new customer
                //
                // Check to see if there is an open teller
                if (isTellerAvailable()) {
                    // The teller serves the customer
                    Teller availableTeller = getAvailableTeller();

                    newCustomer.setTeller(availableTeller.getID());
                    newCustomer.setStartT(current_time);
                    double finish_time = current_time + service_time;
                    newCustomer.setEndT(finish_time);

                    availableTeller.addCust(newCustomer);
                    processedCustomerList.add(newCustomer);

                    customersServed++;
                    // Generate a CompletionLocEvent and place it in FEL
                    CompletionLocEvent next_complete = new CompletionLocEvent(finish_time, availableTeller.getID());
                    futureEventList.offer(next_complete);
                } else {
                    // If fewer then maximum number of customers allowed are already waiting
                    // then the customer will go into a queue
                    if (isCustomerAllowedInBank()) {
                        // The customer goes into a queue
                        LinkedList<Customer> availableQueue = getAvailableQueue();
                        availableQueue.addLast(newCustomer);
                    } else {
                        // The customer will NOT remain in the bank
                        // Do not generate any event
                        unProcessedCustomerList.add(newCustomer);
                        numberTurnedAway++;
                    }
                }

                // Generate the next arrival time
                next_arr_min = current_time + R.exponential(customer_arrival_rate);
                // Is the Bank still open?
                if (next_arr_min <= stop_time) {
                    // If yes then generate a new ArrivalEvent and put it in FEL
                    ArrivalEvent next_arrival = new ArrivalEvent(next_arr_min);
                    futureEventList.offer(next_arrival);
                }
            } else if (currentEvent instanceof CompletionLocEvent) {
                // Get the teller responsible for the completion event
                int tellerId = ((CompletionLocEvent) currentEvent).getLoc();
                Teller currentTeller = tellers.get(tellerId);
                // Remove the customer from the Teller and show the customer her/his way out of the Bank
                currentTeller.removeCust();

                // Add a new customer to the teller
                // Get the queue that is assigned to the teller
                LinkedList<Customer> tellerQueue = getTellerQueue(currentTeller.getID());

                // Proceed only if there is a customer in the queue
                if(isCustomerInQueue(tellerQueue)) {
                    // Get the customer from the queue and remove customer from the queue
                    Customer customerFromQueue = getCustomerFromQueue(tellerQueue);

                    customerFromQueue.setTeller(currentTeller.getID());
                    customerFromQueue.setQueue(getQueueId(currentTeller.getID()));
                    customerFromQueue.setStartT(current_time);

                    currentTeller.addCust(customerFromQueue);

                    // Generate a CompletionLocEvent and place it in FEL
                    double service_time = customerFromQueue.getServiceT();
                    double finish_time = current_time + service_time;
                    customerFromQueue.setEndT(finish_time);

                    processedCustomerList.add(customerFromQueue);
                    customersServed++;

                    CompletionLocEvent next_complete = new CompletionLocEvent(finish_time, currentTeller.getID());
                    futureEventList.offer(next_complete);
                }
            } else {
                // it's a problem
                System.out.println("Error: Unknown event");
            }
        }
    }

    public void showResults() {
        int numberOfQueues = 1;
        if (!queue) numberOfQueues = n_tell;

        int numberWhoWaited = 0;
        int numberWhoWaitToWaitCustomers = 0;
        double totalWaitTime = 0.0;
        double totalServiceTime = 0.0;
        double totalInSystemTime = 0.0;
        double maxWaitTime = 0.0;
        double standardDeviation = 0.0;

        for (Customer processedCustomer : processedCustomerList) {
            if(processedCustomer.getQueue() > -1)
                numberWhoWaited++;

            if(processedCustomer.getWaitT() > 0)
                numberWhoWaitToWaitCustomers++;

            double customerWaitingTime = processedCustomer.getWaitT();
            double customerServiceTime = processedCustomer.getServiceT();
            double customerInSystemTime = processedCustomer.getInSystem();

            totalWaitTime += customerWaitingTime;
            totalServiceTime += customerServiceTime;
            totalInSystemTime += customerInSystemTime;

            if(maxWaitTime < customerWaitingTime)
                maxWaitTime = customerWaitingTime;
        }

        int totalNumberOfCustomers = processedCustomerList.size();

        double averageWaitTime =  totalWaitTime / totalNumberOfCustomers;
        double averageServiceTime =  totalServiceTime / totalNumberOfCustomers;
        double averageInSystemTime =  totalInSystemTime / totalNumberOfCustomers;
        double averageWaitToWaitTime =  totalWaitTime / numberWhoWaitToWaitCustomers;

        for (Customer processedCustomer : processedCustomerList) {
            standardDeviation += Math.pow(processedCustomer.getWaitT() - averageWaitTime, 2);
        }

        double stdDevWait = Math.sqrt(standardDeviation / totalNumberOfCustomers);

        formatAndPrintResults(
                n_tell,
                numberOfQueues,
                maxq,
                arr_rate,
                t_min,
                customerArrivals,
                customersServed,
                numberTurnedAway,
                numberWhoWaited,
                averageWaitTime,
                maxWaitTime,
                stdDevWait,
                averageServiceTime,
                averageWaitToWaitTime,
                averageInSystemTime,
                processedCustomerList,
                unProcessedCustomerList);
    }

    private Teller getAvailableTeller() {
        for (Teller teller: tellers) {
            if(!teller.isBusy()) {
                return teller;
            }
        }

        return null;
    }

    private boolean isTellerAvailable() {
        for (Teller teller: tellers) {
            if(!teller.isBusy()) {
                return true;
            }
        }
        return false;
    }

    private LinkedList<Customer> getAvailableQueue() {
        LinkedList<Customer> shortestQueue = null;
        int minQueueSize = maxq;

        for (LinkedList<Customer> customer_queue: queues) {
            if (minQueueSize >= customer_queue.size()) {
                minQueueSize = customer_queue.size();
                shortestQueue = customer_queue;
            }
        }

        return shortestQueue;
    }

    private boolean isCustomerAllowedInBank() {
        // Count how many customers are in all the queues
        int numberOfCustomersInQueues = 0;
        for (LinkedList<Customer> customer_queue: queues) {
            numberOfCustomersInQueues += customer_queue.size();
        }

        // Count how many customers are being served by tellers
        int numberOfCustomersServedByTellers = 0;
        for (Teller teller: tellers) {
            if(teller.isBusy()) {
                numberOfCustomersServedByTellers += 1;
            }
        }

        return numberOfCustomersServedByTellers + numberOfCustomersInQueues < maxq + n_tell;
    }

    private LinkedList<Customer> getTellerQueue(int tellerId) {
        // If queue = true then use only one queue for all tellers
        if(queue) {
            return queues.get(0);
        } else {
            return queues.get(tellerId);
        }
    }

    private boolean isCustomerInQueue(LinkedList<Customer> tellerQueue) {
        return tellerQueue.size() > 0;
    }

    private Customer getCustomerFromQueue(LinkedList<Customer> tellerQueue) {
        return tellerQueue.removeFirst();
    }

    private int getQueueId(int tellerId) {
        return queue ? 0 : tellerId;
    }

    private void formatAndPrintResults(int numberOfTellers, int numberOfQueues, int maxNumberAllowedToWait,
                                       double customerArrivalRate, double customerServiceTime,
                                       int numberOfCustomersArrived, int numberOfCustomersServed,
                                       int numberTurnedAway, int numberWhoWaited,
                                       double averageWait, double maxWait,
                                       double stdDevWait, double averageService,
                                       double averageWaiterWait, double averageInSystem,
                                       ArrayList<Customer> customerServicedList,
                                       ArrayList<Customer> rejectedCustomerList) {
        System.out.println("Individual customer service information:");
        System.out.println();
        System.out.println("Customer  Arrival    Service  Queue  Teller  Time Serv  Time Cust  Time Serv  Time Spent");
        System.out.println("   Id      Time       Time     Loc    Loc     Begins      Waits      Ends       in Sys");
        System.out.println("--------  -------    -------  -----  ------  ---------  ---------  ---------  ----------");
        for (Customer processedCustomer : customerServicedList) {
            System.out.printf("   %-5d  %6.2f    %7.2f  %5d  %4d    %8.2f  %9.2f  %10.2f  %9.2f\n",
                    processedCustomer.getId(),
                    processedCustomer.getArrivalT(),
                    processedCustomer.getServiceT(),
                    processedCustomer.getQueue(),
                    processedCustomer.getTeller(),
                    processedCustomer.getStartT(),
                    processedCustomer.getWaitT(),
                    processedCustomer.getEndT(),
                    processedCustomer.getInSystem());
        }
        System.out.println();
        System.out.println("Customers who did not stay:");
        System.out.println();
        System.out.println("Customer  Arrival    Service");
        System.out.println("   Id      Time       Time");
        System.out.println("--------  -------    -------");
        for (Customer unProcessedCustomer: rejectedCustomerList) {
            System.out.printf("   %-5d  %7.2f    %7.2f\n",
                    unProcessedCustomer.getId(),
                    unProcessedCustomer.getArrivalT(),
                    unProcessedCustomer.getServiceT());
        }
        System.out.println();
        System.out.println("Number of Tellers: " + numberOfTellers);
        System.out.println("Number of Queues: " + numberOfQueues);
        System.out.println("Max number allowed to wait: " + maxNumberAllowedToWait);
        System.out.printf("Customer arrival rate (per hr): %.1f\n", customerArrivalRate);
        System.out.printf("Customer service time (ave min): %.1f\n", customerServiceTime);
        System.out.println("Number of customers arrived: " + numberOfCustomersArrived);
        System.out.println("Number of customers served: " + numberOfCustomersServed);
        System.out.println("Num. Turned Away: " + numberTurnedAway);
        System.out.println("Num. who waited: " + numberWhoWaited);
        System.out.printf("Average Wait: %.14f min.\n", averageWait);
        System.out.printf("Max Wait: %.14f min.\n", maxWait);
        System.out.printf("Std. Dev. Wait: %.14f\n", stdDevWait);
        System.out.printf("Ave. Service: %.14f min.\n", averageService);
        System.out.printf("Ave. Waiter Wait: %.14f min.\n", averageWaiterWait);
        System.out.printf("Ave. in System: %.14f min.\n", averageInSystem);
        System.out.println();
    }
}