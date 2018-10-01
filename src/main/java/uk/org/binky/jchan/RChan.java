package uk.org.binky.jchan;

public interface RChan<T> extends Comparable<Chan> {
    T recv();
}
