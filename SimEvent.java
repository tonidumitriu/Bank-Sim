public abstract class SimEvent implements Comparable<SimEvent>
{
    protected double e_time;  // Time when event was generated

    public SimEvent(double new_time)
    {
        e_time = new_time;
    }

    public double get_e_time()
    {
        return e_time;
    }

    // This method will allow events to be ordered (ex: via a
    // PriorityQueue)
    public int compareTo(SimEvent right)
    {
        double diff = e_time - right.e_time;
        if (Math.abs(diff) < 0.00001)
            return 0;
        else if (diff < 0)
            return -1;
        else
            return 1;
    }
}
