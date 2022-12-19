package org.go4j;

public class Select {
    public static class Case {
        private final Channel<?> channel;
        private final Runnable runable;

        public Case(Channel<?> channel, Runnable runnable) {
            this.channel = channel;
            this.runable = runnable;
        }
    }

    public static Case of(Channel<?> channel, Runnable runnable) {
        return new Case(channel, runnable);
    }

    public static void on(Runnable def, Case ...cases) {
        for (Case caze : cases) {
            if (caze.channel.hasNext()) {
                caze.runable.run();

                return;
            }
        }

        if (def != null) {
            def.run();
        }
    }
}
