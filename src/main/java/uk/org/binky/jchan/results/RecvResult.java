package uk.org.binky.jchan.results;

public interface RecvResult<T> {
    void run(final T value, final boolean ok);
}
