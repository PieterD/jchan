package uk.org.binky.jchan;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class Chan<T> implements Comparable<Chan>, RChan<T>, SChan<T> {
    private static final AtomicLong idGenerator = new AtomicLong();

    private final long id;
    private final List<RecvTX<T>> recvers = new ArrayList<>();
    private final List<SendTX<T>> senders = new ArrayList<>();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    static <T> Chan<T> asChan(final SChan<T> sch) {
        if (sch instanceof Chan) {
            return (Chan<T>)sch;
        }
        throw new InvalidChannelException("invalid channel type");
    }

    static <T> Chan<T> asChan(final RChan<T> rch) {
        if (rch instanceof Chan) {
            return (Chan<T>)rch;
        }
        throw new InvalidChannelException("invalid channel type");
    }

    public Chan() {
        id = idGenerator.getAndAdd(1);
    }

    public T recv() {
        final var rv = new AtomicReference<T>(null);
        new Select().recv(this, (result, ok) -> {
            rv.set(result);
        }).Go();
        return rv.get();
    }

    public void send(final T value) {
        new Select().send(this, value, null).Go();
    }

    public synchronized void close() {
        if (!closed.compareAndSet(false, true)) {
            throw new ChannelAlreadyClosedException("this channel was already closed");
        }
        for (final RecvTX<T> tx : recvers) {
            tx.tryComplete(null);
        }
        recvers.clear();
        for (final SendTX<T> tx : senders) {
            tx.tryComplete(null);
        }
        senders.clear();
    }

    public RChan asRecvOnly() {
        return this;
    }

    public SChan asSendOnly() {
        return this;
    }

    @Override
    public int compareTo(final Chan o) {
        return Long.compare(this.id, o.id);
    }

    synchronized boolean complete(final SendTX<T> stx) {
        if (closed.get()) {
            throw new SendOnClosedChannelException("attempted to send on a closed channel");
        }
        for (int i = 0; i < recvers.size(); i++) {
            final var rtx = recvers.get(i);
            //TODO: we could remove completed transactions.
            if (rtx.tryComplete(stx)) {
                recvers.remove(i);
                return true;
            }
        }
        return false;
    }

    synchronized boolean complete(final RecvTX<T> rtx) {
        if (closed.get()) {
            rtx.close();
            return true;
        }
        for (int i = 0; i < senders.size(); i++) {
            final var stx = senders.get(i);
            //TODO: we could remove completed transactions.
            if (stx.tryComplete(rtx)) {
                senders.remove(i);
                return true;
            }
        }
        return false;
    }

    synchronized void put(final SendTX<T> stx) {
        senders.add(stx);
    }

    synchronized void put(final RecvTX<T> rtx) {
        recvers.add(rtx);
    }

    synchronized void rem(final SendTX<T> stx) {
        //TODO: this can be improved with a better data type.
        senders.remove(stx);
    }

    synchronized void rem(final RecvTX<T> rtx) {
        //TODO: this can be improved with a better data type.
        recvers.remove(rtx);
    }
}
