package org.go4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@MicronautTest
public class ChannelTest {
    static ExecutorService executorService = Executors.newCachedThreadPool();

    static void run(Runnable run) {
        executorService.submit(run);
    }

    static Callable<?> run(Callable<?> run) {
        executorService.submit(run);

        return run;
    }

    @Test
    void testSimple() {
        Channel<Integer> ch = new Channel<>();

        Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                int value = ch.read();

                assertEquals(42, value);
            }

        }, 100, 100);

        ch.write(42);
    }

    @Test
    void nonBlocking() {
        Channel<Integer> ch = new Channel<>(1);

        ch.write(42);
    }

    @Test
    void fanIn() {
        Channel<Integer> ch = new Channel<>(9);
        Waitgroup wg = new Waitgroup();

        for (int i = 0; i < 3; i++) {
            wg.add(1);

            run(() -> {
                IntStream.range(0, 3).forEach(value -> ch.write(1));

                wg.done();
            });
        }

        wg.waitFor();

        ch.close();

        int sum = ch.stream().collect(Collectors.summingInt(Integer::intValue));

        assertEquals(9,sum);
    }

    @Test
    @Timeout(1)
    void simple() {
        Channel<Integer> ch = new Channel<>();

        assertFalse(ch.isClosed());
        ch.close();
        assertTrue(ch.isClosed());
    }

    @Test
    @Timeout(1)
    void multipleClose() {
        Channel<Integer> ch = new Channel<>();
        Exception thrown = Assertions.assertThrows(ChannelException.class, () -> {
            ch.close();
            ch.close();
        });

        assertEquals(ChannelException.msgClosed, thrown.getMessage());
    }

    @Test
    @Timeout(1)
    void closed() {
        Channel<Integer> ch = new Channel<>();

        assertFalse(ch.isClosed());
        ch.close();
        assertTrue(ch.isClosed());
    }

    @Test
    @Timeout(1)
    void invalid() {
        Exception thrown = Assertions.assertThrows(ChannelException.class, () -> {
            new Channel<>(0, false, false);
        });

        assertEquals(ChannelException.msgInvalid, thrown.getMessage());
    }

    @Test
    @Timeout(1)
    void onlyReadable() {
        Channel<Integer> ch = new Channel<>(0, true, false);

        Exception thrown = Assertions.assertThrows(ChannelException.class, () -> {
            ch.write(42);
        });

        assertEquals(ChannelException.msgNotWritable, thrown.getMessage());
    }

    @Test
    @Timeout(1)
    void onlyWriteable() {
        Channel<Integer> ch = new Channel<>(0, false, true);
        ch.close();

        Exception thrown = Assertions.assertThrows(ChannelException.class, () -> {
            ch.read();
        });

        assertEquals(ChannelException.msgNotReadable, thrown.getMessage());
    }

    @Test
    @Timeout(1)
    void readClosedChannel() {
        Channel<Integer> ch = new Channel<>();
        ch.close();

        assertNull(ch.read());
    }

    @Test
    @Timeout(1)
    void readSimple() {
        Channel<Integer> ch = new Channel<>(0);
        new Thread(() -> {
            ch.write(42);
        }).start();

        int value = ch.read();

        assertEquals(42, value);
    }

    @Test
    @Timeout(1)
    void readBlocking() {
        Channel<Integer> ch = new Channel<>(0);
        new Thread(() -> {
            Go4j.sleep(100l);

            ch.write(42);
        }).start();

        long start = System.currentTimeMillis();

        int value = ch.read();

        assertEquals(42, value);
        assertTrue(System.currentTimeMillis() - start > 100);
    }

    @Test
    @Timeout(1)
    void readList() {
        Channel<Integer> ch = new Channel<>(0);
        new Thread(() -> {
            IntStream.range(0, 1000).forEach(ch::write);
        }).start();

        for (int i = 0; i < 1000; i++) {
            int value = ch.read();

            assertEquals(i, value);
        }
    }

    @Test
    @Timeout(1)
    void readIterator() {
        Channel<Integer> ch = new Channel<>(0);
        new Thread(() -> {
            IntStream.range(0, 3).forEach(ch::write);

            ch.close();
        }).start();

        int i = 0;
        for (Iterator<Integer> iter = ch.iterator(); iter.hasNext(); ) {
            int value = iter.next();

            assertEquals(i, value);

            i++;
        }
    }

    @Test
    @Timeout(1)
    void writeToClosedChannel() {
        Channel<Integer> ch = new Channel<>(0);
        ch.close();

        Exception thrown = Assertions.assertThrows(ChannelException.class, () -> {
            ch.write(42);
        });

        assertEquals(ChannelException.msgClosed, thrown.getMessage());
    }

    @Test
    @Timeout(1)
    void readWriteMultiple() {
        Channel<Integer> ch = new Channel<>(0);

        Waitgroup wg = new Waitgroup();
        for (int i = 0; i < 3; i++) {
            wg.add();
            Go4j.run(() -> {
                IntStream.range(0, 5).forEach(ch::write);
                wg.done();
            });
        }

        Go4j.run(() -> {
            wg.waitFor();

            ch.close();
        });

        int i = 0;
        for (Iterator<Integer> iter = ch.iterator(); iter.hasNext(); ) {
            iter.next();

            i++;
        }

        assertEquals(15, i);
    }

    @Test
    @Timeout(1)
    void readBufferedNonBlocking() {
        Channel<Integer> ch = new Channel<>(1);

        ch.write(42);
    }

    @Test
    @Timeout(1)
    void readBufferedBlocking() {
        Channel<Integer> ch = new Channel<>(1);

        Thread blocker = new Thread(() -> {
            ch.write(42);
            ch.write(4242);
        });
        blocker.start();

        Go4j.sleep(100l);

        assertEquals(Thread.State.WAITING, blocker.getState());

        blocker.interrupt();
    }
}
