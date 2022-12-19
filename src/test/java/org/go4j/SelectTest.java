package org.go4j;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

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
            Select.on(() -> {
                all.close();
            }, cases.toArray(Select.Case[]::new));
        }

        int sum = all.stream().collect(Collectors.summingInt(Integer::intValue));

        assertEquals(9, sum);
    }

    @Test
    void test() {
        Channel<Integer> c0 = new Channel<>(1);
        c0.write(8);
        c0.close();
        Channel<Integer> c1 = new Channel<>(1);
        c1.write(15);
        c1.close();

        Channel<Boolean> quit = new Channel<>();
        Channel<Boolean> timer = Go4j.after(1000);

        while (!quit.isClosed()) {
            Select.on(null,
                      Select.of(c0, () -> System.out.println(c0.read())),
                      Select.of(c1, () -> System.out.println(c1.read())),
                      Select.of(timer,() -> {
                          quit.close();
                      })
            );
        }

        System.out.println("end");
    }
}
