package uk.org.binky.jchan;

public interface RecvResult<T> {
    void run(final T value);
}
