package uk.org.binky.jchan;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

abstract class TX<T> implements Comparable<TX> {
    protected final Chan<T> ch;
    protected final AtomicReference<TX> completer;
    protected final Thread thread;

    TX(final Chan<T> ch, final AtomicReference<TX> completer) {
        this.ch = ch;
        this.completer = completer;
        this.thread = Thread.currentThread();
    }

    @Override
    public int compareTo(final TX o) {
        return this.ch.compareTo(o.ch);
    }

    Chan<T> chan() {
        return ch;
    }

    boolean isComplete() {
        return completer.get() != null;
    }

    abstract boolean quick();

    abstract void runResult();

    abstract void put();

    abstract void rem();
}

class SendTX<T> extends TX<T> {
    private final T value;
    private final SendResult result;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    SendTX(final Chan<T> ch, final AtomicReference<TX> completer, final T value, final SendResult result) {
        super(ch, completer);
        this.value = value;
        this.result = result;
    }

    T getValue() {
        return value;
    }

    // Called by Select to attempt to quickly finish the transaction,
    // without having to put it on the waiting list.
    boolean quick() {
        return ch.complete(this);
    }

    // Called by Chan to attempt to complete the transaction
    // rtx can be null when called from Chan.close.
    boolean tryComplete(final RecvTX<T> rtx) {
        synchronized (thread) {
            if (!completer.compareAndSet(null, this)) {
                return false;
            }
            if (rtx == null) {
                close();
            } else {
                rtx.setValue(getValue());
            }
            thread.notify();
        }
        return true;
    }

    void runResult() {
        if (closed.get()) {
            throw new SendOnClosedChannelException("attempted to send on a closed channel");
        }
        if (result != null) {
            result.run(true);
        }
    }

    private void close() {
        closed.set(true);
    }

    void put() {
        ch.put(this);
    }

    void rem() {
        ch.rem(this);
    }
}

class RecvTX<T> extends TX<T> {
    private final RecvResult<T> result;
    private final AtomicReference<T> value = new AtomicReference<>();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    RecvTX(final Chan<T> ch, final AtomicReference<TX> completer, final RecvResult<T> result) {
        super(ch, completer);
        this.result = result;
    }

    void setValue(final T value) {
        this.value.set(value);
    }

    // Called by Select to attempt to quickly finish the transaction,
    // without having to put it on the waiting list.
    boolean quick() {
        return ch.complete(this);
    }

    // Called by Chan to attempt to complete the transaction.
    // stx can be null when called from Chan.close.
    boolean tryComplete(final SendTX<T> stx) {
        synchronized (thread) {
            if (!completer.compareAndSet(null, this)) {
                return false;
            }
            if (stx == null) {
                close();
            } else {
                setValue(stx.getValue());
            }
            //TODO: Maybe notify instead of notifyAll?
            thread.notifyAll();
        }
        return true;
    }

    void runResult() {
        if (result != null) {
            if (closed.get()) {
                result.run(null, false);
                return;
            }
            result.run(value.get(), true);
        }
    }

    void close() {
        closed.set(true);
    }

    void put() {
        ch.put(this);
    }

    void rem() {
        ch.rem(this);
    }
}
