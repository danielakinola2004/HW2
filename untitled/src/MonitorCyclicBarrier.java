import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/* Use only Java monitors to accomplish the required synchronization */
public class MonitorCyclicBarrier implements CyclicBarrier {

    private final int parties;
    private int idx;           // counts down the number of threads still to arrive
    private int gen = 0;  // changes when barrier trips
    private boolean active = true;

    // The lock and condition variable that act as our monitor.
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();

    public MonitorCyclicBarrier(int parties) {
        this.parties = parties;
        this.idx = 0;
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
            if (!active) {
                return 0;
            }
            int ret = idx++;
            int g = gen;

            if (idx == parties) {
                // Last thread to arrive: condition the barrier.
                gen++;
                idx = 0;
                condition.signalAll();
                return ret;
            }
            // Otherwise, wait until the barrier trips (i.e. gen changes)
            while (g == gen && active) {
                condition.await();
            }
            
            if (!active) {
                return 0;
            }
            return ret;
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
                idx = parties;    
                gen++;      
                condition.signalAll();
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
            condition.signalAll();  // wake all waiting threads so they exit
        } finally {
            lock.unlock();
        }
    }
}
