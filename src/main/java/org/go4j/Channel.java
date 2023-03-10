package org.go4j;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Channel<T> implements AutoCloseable, Iterable<T> {
    private final int count;
    private final List<T> list = new ArrayList<>();
    private final boolean canRead;
    private final boolean canWrite;
    private boolean isClosed = false;

    public Channel() {
        this(0);
    }

    public Channel(int count) {
        this(count, true, true);
    }

    public Channel(int count, boolean canRead, boolean canWrite) {
        if (!canRead && !canWrite) {
            throw ChannelException.invalid();
        }

        this.count = count;
        this.canRead = canRead;
        this.canWrite = canWrite;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return Channel.this.await();
            }

            @Override
            public T next() {
                return read();
            }
        };
    }

    public Stream<T> stream() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<>() {
            @Override
            public boolean hasNext() {
                return Channel.this.await();
            }

            @Override
            public T next() {
                return read();
            }
        }, Spliterator.ORDERED), false);
    }

    public boolean isClosed() {
        synchronized (list) {
            return isClosed;
        }
    }

    @Override
    public void close() {
        synchronized (list) {
            if (isClosed) {
                throw ChannelException.closed();
            }

            isClosed = true;

            list.notify();
        }
    }

    public boolean canRead() {
        return canRead;
    }

    public boolean canWrite() {
        return canWrite;
    }

    private void verifyClosed() {
        synchronized (list) {
            if (isClosed) {
                throw ChannelException.closed();
            }
        }
    }

    private void verifyReadable() throws ChannelException {
        if (!canRead) {
            throw ChannelException.noReadable();
        }
    }

    private void verifyWritable() throws ChannelException {
        if (!canWrite) {
            throw ChannelException.notWritable();
        }
    }

    boolean await() {
        synchronized (list) {
            while (true) {
                if (!list.isEmpty()) {
                    return true;
                }

                if (isClosed) {
                    return false;
                }

                try {
                    list.wait();
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    public void write(T item) throws ChannelException {
        verifyWritable();
        verifyClosed();

        synchronized (list) {
            list.add(item);

            list.notify();

            if (list.size() > count) {
                try {
                    list.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public T read() {
        verifyReadable();

        synchronized (list) {
            while (true) {
                if (list.isEmpty()) {
                    if (isClosed) {
                        return null;
                    }

                    try {
                        list.wait();
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }

                if (!list.isEmpty()) {
                    T value = list.remove(0);

                    list.notify();

                    return value;
                }
            }
        }
    }
}
