package org.go4j;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest
public class SelectTest {
    @Test
    void testSimple() {
        Channel<Integer> all = new Channel<>(9);

        List<Select.Case> cases = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            Channel<Integer> ch = new Channel<>();
            cases.add(new Select.Case(ch, () -> all.write(ch.read())));

            Go4j.run(() -> {
                IntStream.range(0, 3).forEach(ch::write);
                ch.close();
            });
        }

        while (!all.isClosed()) {
            Select.of(all::close, cases.toArray(Select.Case[]::new));
        }

        int sum = all.stream().mapToInt(Integer::intValue).sum();

        assertEquals(9, sum);
    }

    @Test
    void testTimer() {
        Channel<Integer> c0 = new  Channel<>(1);
        c0.write(5);
        c0.close();
        Channel<Integer> c1 = new Channel<>(1);
        c1.write(7);
        c1.close();

        Channel<Boolean> quit = new Channel<>();
        Channel<Boolean> timer = Go4j.after(1000);

        Channel<Integer> all = new Channel<>(9);

        while (!quit.isClosed()) {
            Select.of(null,
                      Select.ofCase(c0, () -> all.write(c0.read())),
                      Select.ofCase(c1, () -> all.write(c1.read())),
                      Select.ofCase(timer, quit::close)
            );
        }

        all.close();

        int sum = all.stream().mapToInt(Integer::intValue).sum();

        assertEquals(12, sum);
    }
}
