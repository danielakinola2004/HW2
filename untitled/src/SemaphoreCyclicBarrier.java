import java.util.concurrent.Semaphore;

public class SemaphoreCyclicBarrier implements CyclicBarrier {
    private final int parties;
    // The count of threads that have entered this round.
    // (When count == parties the first phase is complete.)
    private int count = 0;

    // When active==false, the barrier is “nullified”
    private boolean active = true;

    // mutex protects shared variables (count, active, etc.)
    private final Semaphore mutex = new Semaphore(1);
    // turnstile is the first “gate” that blocks until all threads have arrived.
    // It is normally 0 and gets released parties times when the last thread arrives.
    private final Semaphore turnstile = new Semaphore(0);
    // turnstile2 is the second “gate” that blocks until all threads have left the barrier.
    // It is normally 0 and gets released parties times when the last thread leaves.
    private final Semaphore turnstile2 = new Semaphore(0);

    public SemaphoreCyclicBarrier(int parties) {
        this.parties = parties;
    }

    /**
     * await() makes the calling thread wait until the required number of threads (parties)
     * have called await() – unless the barrier is deactivated.
     * Returns an “arrival index” (with the last thread to arrive getting 0).
     */
    public int await() throws InterruptedException {
        // If the barrier is not active, do not block.
        if (!active)
            return 0;

        int arrivalIndex;

        // === Phase 1: Arrival ===
        mutex.acquire();
        // (Double–check active inside the critical section in case the barrier was deactivated
        // just before this thread acquired the mutex.)
        if (!active) {
            mutex.release();
            return 0;
        }
        // Compute an arrival index so that the very first thread gets (parties-1)
        // and the last thread gets 0.
        arrivalIndex = count;
        count++;
        boolean isLast = (count == parties);
        if (isLast) {
            // Last thread arrived:
            // Open the first gate (turnstile) for all threads.
            for (int i = 0; i < parties; i++) {
                turnstile.release();
            }
        }
        mutex.release();

        // Wait at the first gate.
        turnstile.acquire();

        // If the barrier was deactivated while waiting, simply return.
        if (!active)
            return arrivalIndex;

        // === Phase 2: Departure ===
        mutex.acquire();
        // (If active is false here, then a concurrent deactivation is in progress;
        // in that case, simply let this thread return without further barrier work.)
        if (active) {
            count--;
            boolean isLastLeaving = (count == 0);
            if (isLastLeaving) {
                // Last thread leaving:
                // Open the second gate (turnstile2) for everyone.
                for (int i = 0; i < parties; i++) {
                    turnstile2.release();
                }
            }
            mutex.release();
            // Wait at the second gate.
            turnstile2.acquire();
        } else {
            mutex.release();
        }

        return arrivalIndex;
    }

    /**
     * activate() makes the barrier active again.
     * When activated, new await() calls will block until parties threads have reached the barrier.
     * We also drain any “leftover” permits from the gates.
     */
    public void activate() {
        // Acquire the mutex to restore a clean state.
        mutex.acquireUninterruptibly();
        active = true;
        count = 0;
        // Drain any leftover permits from the semaphores.
        while (turnstile.tryAcquire()) { /* nothing */ }
        while (turnstile2.tryAcquire()) { /* nothing */ }
        mutex.release();
    }

    /**
     * deactivate() disables the barrier.
     * Any thread calling await() after deactivation immediately returns.
     * Also, if any threads are already waiting (or in the middle of a barrier round),
     * they will be released.
     */
    public void deactivate() {
        // Grab the mutex to update shared state.
        mutex.acquireUninterruptibly();
        active = false;
        count = 0; // reset the round in progress, if any
        // Release waiting threads (if any) on both gates.
        turnstile.release(parties);
        turnstile2.release(parties);
        mutex.release();
    }
}
