// SAO993
// DAA3652

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class PriorityQueue {
	public class Node {
		String name;
		int priority;
		Node next;
		final ReentrantLock lock = new ReentrantLock();

		public Node() {
			this.name = "";
			this.priority = Integer.MAX_VALUE;
			this.next = null;
		}
		public Node(String name, int priority) {
			this.name = name;
			this.priority = priority;
			this.next = null;
		}
	}
	private final Node head;
	int capacity;
	int size;
	private ReentrantLock queueLock = new ReentrantLock();
	private final Condition notFull = queueLock.newCondition();
	private final Condition notEmpty = queueLock.newCondition();

	public PriorityQueue(int maxSize) {
        // Creates a Priority queue with maximum allowed size as capacity
		head = new Node();
		this.capacity = maxSize;
		this.size = 0;
	}
	public int add(String name, int priority) {
        // Adds the name with its priority to this queue.
        // Returns the current position in the list where the name was inserted;
        // otherwise, returns -1 if the name is already present in the list.
        // This method blocks when the list is full.
		queueLock.lock();
		try {
			if (search(name) != -1)
				return -1;

			while (size >= capacity)
				notFull.await();

			int pos = 0;
			head.lock.lock();
			Node curr = head;
			Node next = head.next;

			while (next != null && next.priority > priority) {
				next.lock.lock();
				curr.lock.unlock();
				curr = next;
				next = next.next;
				pos++;
			}

			Node newNode = new Node(name, priority);
			newNode.next = next;
			curr.next = newNode;

			curr.lock.unlock();  // Unlock the last acquired lock
			this.size++;

			notEmpty.signal();
			return pos;
		} catch (InterruptedException e) {
			return -1;
		} finally {
			queueLock.unlock();
		}

	}
	public void printList() {
		head.lock.lock();
		Node curr = head.next;
		while (curr != null) {
			System.out.print(curr.name + " " + curr.priority + " -> ");
			curr = curr.next;
		}
		head.lock.unlock();
		System.out.println("null");
	}
	public int search(String name) {
        // Returns the position of the name in the list;
        // otherwise, returns -1 if the name is not found.
		head.lock.lock();
		Node curr = head;
		Node next = head.next;
		int pos = 0;

		while (next != null && !next.name.equals(name)) {
			next.lock.lock();
			curr.lock.unlock();
			curr = next;
			next = next.next;
			pos++;
		}

		curr.lock.unlock();

		if (next == null) {
			return -1;
		} else {
			return pos;
		}
	}
	public String getFirst() {
        // Retrieves and removes the name with the highest priority in the list,
        // or blocks the thread if the list is empty.
		queueLock.lock();
		try {
			while (size == 0) {
				notEmpty.await();
			}

			head.lock.lock();
			Node first = head.next;

			if (first == null) {
				head.lock.unlock();
				return null;
			}

			Node next = first.next;
			first.lock.lock();
			next.lock.lock();

			head.next = next;
			first.next = null;

			next.lock.unlock();
			head.lock.unlock();
			first.lock.unlock();

			this.size --;
			notFull.signal();

			return first.name;
		} catch (InterruptedException e) {
			return null;
		} finally {
			queueLock.unlock();
		}
	}
}