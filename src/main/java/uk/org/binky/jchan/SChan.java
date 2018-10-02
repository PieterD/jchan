package uk.org.binky.jchan;

public interface SChan<T> extends Comparable<Chan> {
    void send(final T value);

    void close();
}
