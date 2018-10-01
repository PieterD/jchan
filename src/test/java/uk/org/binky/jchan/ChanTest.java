package uk.org.binky.jchan;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class ChanTest {
    private Thread range(final Chan<Integer> ch, final Chan<Integer> stop) {
        return range(ch, stop, "range");
    }

    private Thread range(final Chan<Integer> ch, final Chan<Integer> stop, final String name) {
        final Thread t = new Thread(() -> {
            final var num = new AtomicInteger();
            final var end = new AtomicBoolean();
            while (!end.get()) {
                new Select()
                        .send(ch, num.get(), (ok) -> {
                            num.addAndGet(1);
                        })
                        .recv(stop, (result, ok) -> {
                            end.set(true);
                        })
                        .Go();
            }
        }, name);
        t.start();
        return t;
    }

    private void join(final Thread t) {
        try {
            t.join();
        } catch (final InterruptedException e) {
            fail("interrupted exception!");
        }
    }

    @Test
    public void testSimpleRange() {
        final var stop = new Chan<Integer>();
        final var ch = new Chan<Integer>();
        final var t = range(ch, stop);
        for (int i = 0; i < 99999; i++) {
            assertEquals(Integer.valueOf(i), ch.recv());
        }
        stop.close();
        new Select()
                .recv(ch, (result, ok) -> {
                    fail("got result after stopping thread");
                })
                .def(() -> {
                })
                .Go();
        join(t);
    }

    @Test
    public void testMultiRangeClose() {
        final var stop = new Chan<Integer>();
        final var ch1 = new Chan<Integer>();
        final var ch2 = new Chan<Integer>();
        final var num1 = new AtomicInteger();
        final var num2 = new AtomicInteger();
        final var t1 = range(ch1, stop, "range1");
        final var t2 = range(ch2, stop, "range2");
        while (num1.get() < 9999 && num2.get() < 9999) {
            new Select()
                    .recv(ch1, (result, ok) -> {
                        final var i = num1.getAndAdd(1);
                        assertEquals(Integer.valueOf(i), result);
                    })
                    .recv(ch2, (result, ok) -> {
                        final var i = num2.getAndAdd(1);
                        assertEquals(Integer.valueOf(i), result);
                    })
                    .Go();
        }
        stop.close();
        new Select()
                .recv(ch1, (result, ok) -> {
                    fail("got result after stopping thread 1");
                })
                .recv(ch2, (result, ok) -> {
                    fail("got result after stopping thread 2");
                })
                .def(() -> {
                })
                .Go();
        assertNull(stop.recv());
        join(t1);
        join(t2);
    }
}
