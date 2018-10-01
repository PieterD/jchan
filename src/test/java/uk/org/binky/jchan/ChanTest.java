package uk.org.binky.jchan;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ChanTest {
    @Test
    public void testSelect() {
        var ch = new Chan<Integer>();
        var stop = new Chan<Integer>();
        final Thread t = new Thread(() -> {
            final var num = new AtomicInteger();
            final var end = new AtomicBoolean();
            while (!end.get()) {
                new Select()
                        .send(ch, num.get(), () -> {
                            num.addAndGet(1);
                        })
                        .recv(stop, (result) -> {
                            end.set(true);
                        })
                        .Go();
            }
        }, "sender");
        t.start();
        for (int i=0; i<99999; i++) {
            assertEquals(Integer.valueOf(i), ch.recv());
        }
        stop.send(0);
        new Select()
                .recv(ch, (result)->{
                    fail("got result after stopping thread");
                })
                .def(() -> {})
                .Go();
        try {
            t.join();
        } catch (final InterruptedException e) {
            fail("interrupted exception!");
        }
    }
}
