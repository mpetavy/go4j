package org.go4j;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Go4j {
    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    public static Future<?> go(Runnable task) {
        return executorService.submit(task);
    }

    public static Future<?> go(Callable<?> task) {
        return executorService.submit(task);
    }

    public static Thread async(Runnable task) {
        Thread thread = new Thread(task);
        thread.start();

        return thread;
    }


    public static void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static Channel<Boolean> after(long milliseconds) {
        Channel<Boolean> ch = new Channel<>();

        go(() -> {
            sleep(milliseconds);
            ch.write(true);
        });

        return ch;
    }
}
