package org.go4j;

import java.util.stream.IntStream;

public class Select {
    public static class Case {
        private final Channel<?> channel;
        private final Runnable runable;

        public Case(Channel<?> channel, Runnable runnable) {
            this.channel = channel;
            this.runable = runnable;
        }
    }

    public static Case ofCase(Channel<?> channel, Runnable runnable) {
        return new Case(channel, runnable);
    }

    private static void shuffle(int[] arr) {
        for (int i = 0;i < arr.length;i++) {
            int rnd = (int)(Math.random() * arr.length);

            int temp = arr[i];

            arr[i] = arr[rnd];
            arr[rnd] = temp;
        }
    }

    public static void of(Runnable def, Case ...cazes) {
        int[] arr = IntStream.range(0, cazes.length).toArray();

        shuffle(arr);

        for (int i = 0;i < arr.length;i++) {
            Case caze = cazes[i];

            if (caze.channel.waitFor()) {
                caze.runable.run();

                return;
            }
        }

        if (def != null) {
            def.run();
        }
    }
}