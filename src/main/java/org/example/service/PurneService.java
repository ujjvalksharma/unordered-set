package org.example.service;

import org.example.model.ExiryPair;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class PurneService {

  private static final PurneService INSTANCE = new PurneService();
  private final PriorityBlockingQueue<ExiryPair<?>> queue = new PriorityBlockingQueue<>();

  private final ReentrantLock lock = new ReentrantLock();
  private final Condition notEmpty = lock.newCondition();
  private final Thread thread;

  private PurneService() {
    this.thread = new Thread(this::cleanUp);
    thread.setDaemon(true);
    thread.start();
  }

  private void cleanUp() {
    while (true) {
      lock.lock();
      try {
        /*
No, you should NOT change while to if
Why?
Because await() can wake up even when not signaled — these are called spurious wakeups.
The JVM specification allows threads to wake up from await() randomly —
so after waking up, the condition (queue.isEmpty()) might still be true.
If you use if, and the thread wakes up spuriously:
It skips the check.
It assumes there’s data.
It calls queue.peek() or queue.poll() and might get null or behave incorrectly.

         */
        while (queue.isEmpty()) {
          notEmpty.await(); // wait until item is added
        }

        ExiryPair<?> next = queue.peek();
        if (next == null) continue;

        long now = System.currentTimeMillis();
        long waitTime = next.getTimeMilli() - now;

        if (waitTime <= 0) {
          queue.poll(); // remove from queue
          next.getMap().remove(next.getT());
        } else {
          // Wait until the next item expires, or gets interrupted
          notEmpty.wait(waitTime);// convert ms to ns
        }

      } catch (InterruptedException e) {
        Thread.currentThread().interrupt(); // preserve interrupt flag
        break; // exit loop
      } finally {
        lock.unlock();
      }
    }
  }

  public static PurneService getInstance() {
    return INSTANCE;
  }

  public void addItem(ExiryPair<?> exiryPair) {
    lock.lock();
    try {
      queue.add(exiryPair);
      notEmpty.signal(); // signal the cleaner thread
    } finally {
      lock.unlock();
    }
  }
}

/*

public class PurneService {

    private static final PurneService INSTANCE = new PurneService();
    private final Object lock = new Object();
    private final Thread thread;

    private final PriorityBlockingQueue<ExiryPair<?>> queue = new PriorityBlockingQueue<>();

    private PurneService() {
        this.thread = new Thread(cleanUp());
        thread.setDaemon(true);
        thread.start();
    }

    private Runnable cleanUp() {
        return () -> {
            while (true) {
                synchronized (lock) {
                    try {
                        while (queue.isEmpty()) {
                            lock.wait();
                        }

                        long now = System.currentTimeMillis();
                        ExiryPair<?> next = queue.peek();

                        if (next.getTimeMilli() <= now) {
                            queue.poll();
                            next.getMap().remove(next.getT());
                        } else {
                            long waitTime = next.getTimeMilli() - now;
                            lock.wait(waitTime);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        };
    }

    public static PurneService getInstance() {
        return INSTANCE;
    }

    public void addItem(ExiryPair<?> exiryPair) {
        queue.add(exiryPair);
        synchronized (lock) {
            lock.notify();
        }
    }
}

 */