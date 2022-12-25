package org.go4j;

public class Waitgroup {
    private final Object lock = new Object();
    private int count = 0;

    public void add() {
        add(1);
    }

    public void add(int count) {
        synchronized (lock) {
            this.count += count;
        }
    }

    public void done() {
        synchronized (lock) {
            count--;

            if (count == 0) {
                lock.notify();
            }
        }
    }

    public void await() {
        synchronized (lock) {
            if (count == 0) {
                return;
            }

            try {
                lock.wait();
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
