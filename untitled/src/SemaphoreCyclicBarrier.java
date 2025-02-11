import java.util.concurrent.Semaphore;

public class SemaphoreCyclicBarrier implements CyclicBarrier {
    private final int parties;
    private int idx = 0;

    private boolean active = true;

    private final Semaphore mutex = new Semaphore(1);
    private final Semaphore notFull = new Semaphore(0);
    private final Semaphore full = new Semaphore(0);

    public SemaphoreCyclicBarrier(int parties) {
        this.parties = parties;
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
        if (!active)
            return 0;
        int ret;
        mutex.acquire();
        if (!active) {
            mutex.release();
            return 0;
        }
        ret = idx;
        idx++;
   
        if (idx == parties) {
            for (int i = 0; i < parties; i++) {
                notFull.release();
            }
        }
        mutex.release();
        notFull.acquire();
        if (!active)
            return ret;
        mutex.acquire();
        if (active) {
            idx--;
            boolean isLastLeaving = (idx == 0);
            if (isLastLeaving) {
                // Last thread leaving:
                // Open the second gate for everyone.
                for (int i = 0; i < parties; i++) {
                    full.release();
                }
            }
            mutex.release();
            full.acquire();
        } else {
            mutex.release();
        }
        return ret;
    }

    /*
     * This method activates the cyclic barrier. If it is already in
     * the active state, no change is made.
     * If the barrier is in the inactive state, it is activated and
     * the state of the barrier is reset to its initial value.
     */
    public void activate() {
        mutex.acquireUninterruptibly();
        active = true;
        idx = 0;
        // Drain any leftover 
        while (notFull.tryAcquire()) {}
        while (full.tryAcquire()) {}
        mutex.release();
    }

    /*
     * This method deactivates the cyclic barrier.
     * It also releases any waiting threads.
     */
    public void deactivate() {
        mutex.acquireUninterruptibly();
        active = false;
        idx = 0;
        notFull.release(parties);
        full.release(parties);
        mutex.release();
    }
}
