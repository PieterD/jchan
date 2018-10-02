package uk.org.binky.jchan.exceptions;

public class InvalidChannelException extends RuntimeException {
    public InvalidChannelException(final String msg) {
        super(msg);
    }
}
