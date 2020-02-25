public class CompletionEvent extends SimEvent
{
    public CompletionEvent(double new_time)
    {
        super(new_time);
    }

    public String toString()
    {
        return "CompletionEvent: " + e_time;
    }
}
