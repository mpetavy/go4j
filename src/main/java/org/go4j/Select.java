package org.go4j;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class Select {
    public static class Case implements Runnable {
        private final Channel<?> channel;
        private final Runnable runable;
        private Channel<Case> response;

        public Case(Channel<?> channel, Runnable runnable) {
            this.channel = channel;
            this.runable = runnable;
        }

        void setResponse(Channel<Case> response) {
            this.response = response;
        }

        public void run() {
            if (channel.await() && !Thread.interrupted()) {
                response.write(this);
            }
        }
    }

    public static Case ofCase(Channel<?> channel, Runnable runnable) {
        return new Case(channel, runnable);
    }

    private static void shuffle(int[] arr) {
        for (int i = 0; i < arr.length; i++) {
            int rnd = (int) (Math.random() * arr.length);

            int temp = arr[i];

            arr[i] = arr[rnd];
            arr[rnd] = temp;
        }
    }

    public static void of(Runnable def, Case... cazes) {
        if (def != null) {
            int[] arr = IntStream.range(0, cazes.length).toArray();

            shuffle(arr);

            for (int i = 0; i < arr.length; i++) {
                Case caze = cazes[i];

                if (caze.channel.await()) {
                    caze.runable.run();

                    return;
                }
            }

            def.run();
        } else {
            Channel<Case> response = new Channel<>(cazes.length);
            List<Thread> threads = new ArrayList<>();


            for (Case caze : cazes) {
                caze.setResponse(response);

                Thread thread = new Thread(caze);
                threads.add(thread);

                thread.start();
            }

            Case cazeToRun = response.read();

            for (Thread thread : threads) {
                thread.interrupt();
            }

            cazeToRun.runable.run();
        }
    }
}