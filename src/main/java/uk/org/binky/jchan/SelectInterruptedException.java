package uk.org.binky.jchan;

public class SelectInterruptedException extends RuntimeException {
    public SelectInterruptedException(final String msg) {
        super(msg);
    }

    public SelectInterruptedException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}
