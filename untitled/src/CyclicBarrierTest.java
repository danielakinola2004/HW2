import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class CyclicBarrierTest  {
	public static void main(String[] args) throws Exception {
		testBarrierTrip();
		testMultipleCycles();
		testDeactivation();
		testActivation();
		testAwaitWhenInactive();
		testThreadInterruption();
		System.out.println("All tests passed.");
	}

	/**
	 * Test that with the correct number of threads the barrier trips
	 * and each thread receives a unique arrival index (0 ... parties-1).
	 */
	public static void testBarrierTrip() throws Exception {
		final int parties = 3;
		final MonitorCyclicBarrier barrier = new MonitorCyclicBarrier(parties);
		final List<Integer> results = Collections.synchronizedList(new ArrayList<>());
		Thread[] threads = new Thread[parties];

		for (int i = 0; i < parties; i++) {
			threads[i] = new Thread(() -> {
				try {
					int res = barrier.await();
					results.add(res);
				} catch (InterruptedException e) {
					results.add(-1);
				}
			});
			threads[i].start();
		}

		for (Thread t : threads) {
			t.join();
		}

		Collections.sort(results);
		List<Integer> expected = Arrays.asList(0, 1, 2);
		if (!results.equals(expected)) {
			throw new AssertionError("testBarrierTrip: expected " + expected + " but got " + results);
		}
	}

	/**
	 * Test that the barrier works correctly over multiple cycles.
	 */
	public static void testMultipleCycles() throws Exception {
		final int parties = 3;
		final MonitorCyclicBarrier barrier = new MonitorCyclicBarrier(parties);
		int cycles = 3;

		for (int cycle = 0; cycle < cycles; cycle++) {
			final List<Integer> results = Collections.synchronizedList(new ArrayList<>());
			Thread[] threads = new Thread[parties];
			for (int i = 0; i < parties; i++) {
				threads[i] = new Thread(() -> {
					try {
						int res = barrier.await();
						results.add(res);
					} catch (InterruptedException e) {
						results.add(-1);
					}
				});
				threads[i].start();
			}
			for (Thread t : threads) {
				t.join();
			}
			Collections.sort(results);
			List<Integer> expected = Arrays.asList(0, 1, 2);
			if (!results.equals(expected)) {
				throw new AssertionError("testMultipleCycles (cycle " + cycle +
						"): expected " + expected + " but got " + results);
			}
		}
	}

	/**
	 * Test that threads waiting in the barrier are released when it is deactivated.
	 * Also, if a thread calls await() when the barrier is inactive, it returns immediately.
	 */
	public static void testDeactivation() throws Exception {
		final int parties = 3;
		final MonitorCyclicBarrier barrier = new MonitorCyclicBarrier(parties);
		final List<Integer> results = Collections.synchronizedList(new ArrayList<>());
		// Use a latch to try to ensure that two threads have reached await().
		CountDownLatch startLatch = new CountDownLatch(2);

		Thread t1 = new Thread(() -> {
			try {
				startLatch.countDown();
				int res = barrier.await();
				results.add(res);
			} catch (InterruptedException e) {
				results.add(-1);
			}
		});
		Thread t2 = new Thread(() -> {
			try {
				startLatch.countDown();
				int res = barrier.await();
				results.add(res);
			} catch (InterruptedException e) {
				results.add(-1);
			}
		});
		t1.start();
		t2.start();

		// Wait a short while to allow t1 and t2 to block in await()
		startLatch.await(500, TimeUnit.MILLISECONDS);
		barrier.deactivate();

		t1.join(1000);
		t2.join(1000);

		if (results.size() != 2) {
			throw new AssertionError("testDeactivation: Expected 2 results, got " + results.size());
		}
		for (int res : results) {
			if (res != 0) {
				throw new AssertionError("testDeactivation: Expected result 0 (deactivated barrier) but got " + res);
			}
		}
		// Verify that calling await on an inactive barrier returns 0 immediately.
		int res = barrier.await();
		if (res != 0) {
			throw new AssertionError("testDeactivation: Await on inactive barrier did not return 0, got " + res);
		}
	}

	/**
	 * Test that after deactivation the barrier can be reactivated and then works normally.
	 */
	public static void testActivation() throws Exception {
		final int parties = 3;
		final MonitorCyclicBarrier barrier = new MonitorCyclicBarrier(parties);
		barrier.deactivate();  // Make sure barrier is inactive
		barrier.activate();    // Reactivate and reset it

		final List<Integer> results = Collections.synchronizedList(new ArrayList<>());
		Thread[] threads = new Thread[parties];
		for (int i = 0; i < parties; i++) {
			threads[i] = new Thread(() -> {
				try {
					int res = barrier.await();
					results.add(res);
				} catch (InterruptedException e) {
					results.add(-1);
				}
			});
			threads[i].start();
		}
		for (Thread t : threads) {
			t.join();
		}
		Collections.sort(results);
		List<Integer> expected = Arrays.asList(0, 1, 2);
		if (!results.equals(expected)) {
			throw new AssertionError("testActivation: expected " + expected + " but got " + results);
		}
	}

	/**
	 * Test that if the barrier is inactive, await returns immediately.
	 */
	public static void testAwaitWhenInactive() throws Exception {
		final MonitorCyclicBarrier barrier = new MonitorCyclicBarrier(3);
		barrier.deactivate();
		long start = System.currentTimeMillis();
		int res = barrier.await();
		long elapsed = System.currentTimeMillis() - start;
		if (res != 0) {
			throw new AssertionError("testAwaitWhenInactive: expected 0 but got " + res);
		}
		if (elapsed > 100) { // if too long, then it did not return immediately
			throw new AssertionError("testAwaitWhenInactive: await did not return immediately when barrier is inactive.");
		}
	}

	/**
	 * Test that a thread waiting in await() can be interrupted and that it throws InterruptedException.
	 * Also verifies that a new barrier instance works normally after interruption.
	 */
	public static void testThreadInterruption() throws Exception {
		// Test interruption on a barrier with 2 parties.
		final MonitorCyclicBarrier barrier = new MonitorCyclicBarrier(2);
		final AtomicBoolean interruptedCaught = new AtomicBoolean(false);

		Thread t = new Thread(() -> {
			try {
				barrier.await();
			} catch (InterruptedException e) {
				interruptedCaught.set(true);
			}
		});
		t.start();
		// Sleep briefly to ensure t is waiting.
		Thread.sleep(500);
		t.interrupt();
		t.join(1000);
		if (!interruptedCaught.get()) {
			throw new AssertionError("testThreadInterruption: thread did not catch InterruptedException");
		}

		// Now verify that a new barrier works normally.
		final MonitorCyclicBarrier barrier2 = new MonitorCyclicBarrier(2);
		final List<Integer> results = Collections.synchronizedList(new ArrayList<>());
		Thread t2 = new Thread(() -> {
			try {
				int res = barrier2.await();
				results.add(res);
			} catch (InterruptedException e) {
				results.add(-1);
			}
		});
		t2.start();
		int resMain = barrier2.await();
		t2.join();
		results.add(resMain);
		Collections.sort(results);
		List<Integer> expected = Arrays.asList(0, 1);
		if (!results.equals(expected)) {
			throw new AssertionError("testThreadInterruption: new barrier expected " + expected + " but got " + results);
		}
	}
}