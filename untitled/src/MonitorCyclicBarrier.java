import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/* Use only Java monitors to accomplish the required synchronization */
public class MonitorCyclicBarrier implements CyclicBarrier {

    private final int parties;
    private int count;           // counts down the number of threads still to arrive
    private int generation = 0;  // changes when barrier trips
    private boolean active = true;

    // The lock and condition variable that act as our monitor.
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition trip = lock.newCondition();

    public MonitorCyclicBarrier(int parties) {
        this.parties = parties;
        this.count = parties;
    }

    /*
     * An active CyclicBarrier waits until all parties have invoked
     * await on this CyclicBarrier. If the current thread is not
     * the last to arrive then it is disabled for thread scheduling
     * purposes and lies dormant until the last thread arrives.
     * An inactive CyclicBarrier does not block the calling thread. It
     * instead allows the thread to proceed by immediately returning.
     * Returns: the arrival index of the current thread, where index 0
     * indicates the first to arrive and (parties-1) indicates the last to arrive.
     */
    public int await() throws InterruptedException {
        lock.lock();
        try {
            // If the barrier is inactive, do not block.
            if (!active) {
                return 0;
            }

            // Compute the arrival index (first thread gets 0, next 1, etc.)
            int arrivalIndex = parties - count;
            int myGeneration = generation;

            // Decrement count since this thread has arrived.
            if (--count == 0) {
                // Last thread to arrive: trip the barrier.
                generation++;         // move to a new generation
                count = parties;      // reset count for next use
                trip.signalAll();     // wake up all waiting threads
                return arrivalIndex;
            }

            // Otherwise, wait until the barrier trips (i.e. generation changes)
            while (myGeneration == generation && active) {
                trip.await();
            }

            // If the barrier has been deactivated while waiting, return 0.
            if (!active) {
                return 0;
            }

            return arrivalIndex;
        } finally {
            lock.unlock();
        }
    }

    /*
     * This method activates the cyclic barrier. If it is already in
     * the active state, no change is made.
     * If the barrier is in the inactive state, it is activated and
     * the state of the barrier is reset to its initial value.
     */
    public void activate() throws InterruptedException {
        lock.lock();
        try {
            if (!active) {
                active = true;
                count = parties;    // reset the barrier count
                generation++;       // move to a new generation to wake waiting threads
                trip.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    /*
     * This method deactivates the cyclic barrier.
     * It also releases any waiting threads.
     */
    public void deactivate() throws InterruptedException {
        lock.lock();
        try {
            active = false;
            trip.signalAll();  // wake all waiting threads so they can exit promptly
        } finally {
            lock.unlock();
        }
    }
}
