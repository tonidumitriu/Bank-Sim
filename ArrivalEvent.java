public class ArrivalEvent extends SimEvent
{
    public ArrivalEvent(double new_time)
    {
        super(new_time);
    }

    public String toString()
    {
        return "ArrivalEvent: " + e_time;
    }
}
