public class CompletionLocEvent extends CompletionEvent
{
    private int location;

    public CompletionLocEvent(double new_time, int loc)
    {
        super(new_time);
        location = loc;
    }

    public int getLoc()
    {
        return location;
    }

    public String toString()
    {
        return super.toString() + " at location " + location;
    }
}
