package org.go4j;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

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
        try (Channel<Integer> ch = new Channel<>()) {

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
    }

    @Test
    void nonBlocking() {
        try (Channel<Integer> ch = new Channel<>(1)) {

            ch.write(42);
        }
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

        wg.await();

        ch.close();

        int sum = ch.stream().mapToInt(Integer::intValue).sum();

        assertEquals(9, sum);
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
        Exception thrown = Assertions.assertThrows(ChannelException.class, () -> new Channel<>(0, false, false));

        assertEquals(ChannelException.msgInvalid, thrown.getMessage());
    }

    @Test
    @Timeout(1)
    void onlyReadable() {
        try (Channel<Integer> ch = new Channel<>(0, true, false)) {

            Exception thrown = Assertions.assertThrows(ChannelException.class, () -> ch.write(42));

            assertEquals(ChannelException.msgNotWritable, thrown.getMessage());
        }
    }

    @Test
    @Timeout(1)
    void onlyWriteable() {
        Channel<Integer> ch = new Channel<>(0, false, true);
        ch.close();

        Exception thrown = Assertions.assertThrows(ChannelException.class, ch::read);

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
        try (Channel<Integer> ch = new Channel<>(0)) {

            Go4j.go(() -> ch.write(42));

            int value = ch.read();

            assertEquals(42, value);
        }
    }

    @Test
    @Timeout(1)
    void readBlocking() {
        try (Channel<Integer> ch = new Channel<>(0)) {

            Go4j.go(() -> {
                Go4j.sleep(100L);

                ch.write(42);
            });

            long start = System.currentTimeMillis();

            int value = ch.read();

            assertEquals(42, value);
            assertTrue(System.currentTimeMillis() - start > 100);
        }
    }

    @Test
    @Timeout(1)
    void readList() {
        try (Channel<Integer> ch = new Channel<>(0)) {

            Go4j.go(() -> IntStream.range(0, 1000).forEach(ch::write));

            for (int i = 0; i < 1000; i++) {
                int value = ch.read();

                assertEquals(i, value);
            }
        }
    }

    @Test
    @Timeout(1)
    void readIterator() {
        Channel<Integer> ch = new Channel<>(0);

        Go4j.go(() -> {
            IntStream.range(0, 3).forEach(ch::write);

            ch.close();
        });

        int i = 0;
        for (int value : ch) {
            assertEquals(i, value);

            i++;
        }
    }

    @Test
    @Timeout(1)
    void writeToClosedChannel() {
        Channel<Integer> ch = new Channel<>(0);
        ch.close();

        Exception thrown = Assertions.assertThrows(ChannelException.class, () -> ch.write(42));

        assertEquals(ChannelException.msgClosed, thrown.getMessage());
    }

    @Test
    @Timeout(1)
    void readWriteMultiple() {
        Channel<Integer> ch = new Channel<>(0);

        Waitgroup wg = new Waitgroup();
        for (int i = 0; i < 3; i++) {
            wg.add();
            Go4j.go(() -> {
                IntStream.range(0, 5).forEach(ch::write);
                wg.done();
            });
        }

        Go4j.go(() -> {
            wg.await();

            ch.close();
        });

        int i = 0;
        for (Integer integer : ch) {
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

        Thread blocker = Go4j.async(() -> {
            ch.write(42);
            ch.write(4242);
        });

        Go4j.sleep(100L);

        assertEquals(Thread.State.WAITING, blocker.getState());

        blocker.interrupt();
    }
}
