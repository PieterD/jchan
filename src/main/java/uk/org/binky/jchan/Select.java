package uk.org.binky.jchan;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class Select {
    private final AtomicReference<TX> completer = new AtomicReference<>(null);
    private final List<TX> transactions = new ArrayList<>();
    private DefaultResult defaultResult;
    private boolean multipleDefaults = false;

    public <T> Select send(final Chan<T> ch, final T value, final SendResult r) {
        transactions.add(new SendTX<T>(ch, completer, value, r));
        return this;
    }

    public <T> Select recv(final Chan<T> ch, final RecvResult<T> r) {
        transactions.add(new RecvTX<T>(ch, completer, r));
        return this;
    }

    public Select Default(final DefaultResult r) {
        if (defaultResult != null) {
            multipleDefaults = true;
        }
        defaultResult = r;
        return this;
    }

    private void validate() {
        if (multipleDefaults) {
            throw new SelectException("Select has multiple defaults");
        }
        if (transactions.size() == 0) {
            throw new SelectException("Select has no send or recv statements");
        }
        final Set<Chan<?>> set = new HashSet<>();
        for (final TX transaction : transactions) {
            final var ch = transaction.chan();
            if (set.contains(ch)) {
                throw new SelectException("Select has multiple instances of the same channel");
            }
            set.add(ch);
        }
    }

    public void Go() {
        validate();
        // Fast-path; try all channels one by one in random order.
        Collections.shuffle(transactions);
        for (final TX transaction : transactions) {
            if (transaction.quick()) {
                transaction.runResult();
                return;
            }
        }
        // No transactions can be completed immediately; default if present.
        if (defaultResult != null) {
            defaultResult.run();
            return;
        }
        // No default statement, so we block.
        // To do that, we have to register our transactions
        // with all channels, and to do that we need to lock them all.
        final var tx = lockChannels(() -> {
            // One last chance to prevent us from sleeping;
            // someone may have come in before we locked all the channels.
            for (final TX transaction : transactions) {
                if (transaction.quick()) {
                    // We must return it so we can run the result without any locks.
                    return transaction;
                }
            }
            // Add transactions to all channels.
            for (final TX transaction : transactions) {
                transaction.put();
            }
            return null;
        });
        if (tx != null) {
            tx.runResult();
            return;
        }
        // Blocking loop.
        final Thread thread = Thread.currentThread();
        while (completer.get() == null) {
            try {
                synchronized (thread) {
                    thread.wait();
                }
            } catch (final InterruptedException e) {
                throw new SelectInterruptedException("Select was interrupted", e);
            }
            if (thread.isInterrupted()) {
                throw new SelectInterruptedException("Select was interrupted");
            }
        }
        // We don't have to lock all the channels to remove stale transactions.
        for (final TX transaction : transactions) {
            transaction.rem();
        }
        // And we're done.
        completer.get().runResult();
    }

    private TX lockChannels(final LockRunner r) {
        // Sort on channels to prevent deadlock
        Collections.sort(transactions);
        return lockRecurse(0, () -> {
            //TODO: do we always want this? maybe move this to within the Runnable.
            Collections.shuffle(transactions);
            return r.run();
        });
    }

    private TX lockRecurse(final int i, final LockRunner r) {
        if (i >= transactions.size()) {
            return r.run();
        }
        final var tx = transactions.get(i);
        synchronized (tx.chan()) {
            return lockRecurse(i + 1, r);
        }
    }
}