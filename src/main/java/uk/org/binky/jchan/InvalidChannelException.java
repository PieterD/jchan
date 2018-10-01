package uk.org.binky.jchan;

public class InvalidChannelException extends RuntimeException {
    public InvalidChannelException(final String msg) {
        super(msg);
    }
}
