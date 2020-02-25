// CS 0445 Spring 2020
// SimpleQueue<T> interface

public interface SimpleQueue<T>
{
    public boolean offer(T element); // or add() or enqueue()
    // add a new element at the logical end of the Queue
    // return true if add is successful and false otherwise

    public T poll(); // or remove() or dequeue()
    // remove the element at the logical front of the Queue
    // return the element or null if the Queue is empty

    public T peek(); // or getFront()
    // get and return element at logical front of the Queue
    // do not remove the element
    // return null if Queue is empty

    public boolean isEmpty();
    // return true if Queue is empty; false otherwise

    public void clear();
    // clear all contents from Queue and set to empty
}