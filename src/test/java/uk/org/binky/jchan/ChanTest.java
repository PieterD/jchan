package uk.org.binky.jchan;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class ChanTest {
//    @Test
//    public void testBasic() {
//        var ch1 = new Chan<Integer>();
//        var ch2 = new Chan<String>();
//        new Thread(() -> ch1.Send(654)).start();
//
//        var result = ch1.Recv();
//        assertEquals(new Integer(654), result);
//    }

    @Test
    public void testSelect() {
        var ch = new Chan<Integer>();
        var stop = new Chan<Integer>();
        new Thread(() -> {
            final var num = new AtomicInteger();
            final var end = new AtomicBoolean();
            while (!end.get()) {
                System.out.println("send");
                new Select()
                        .send(ch, num.get(), () -> {
                            num.addAndGet(1);
                        })
//                        .recv(stop, (result) -> {
//                            end.set(true);
//                        })
                        .Go();
            }
        }, "sender").start();
        for (int i=0; i<999; i++) {
            System.out.println("recv");
            assertEquals(Integer.valueOf(i), ch.recv());
        }
    }
}
