package uk.org.binky.jchan.exceptions;

public class ChannelAlreadyClosedException extends RuntimeException {
    public ChannelAlreadyClosedException(final String msg) {
        super(msg);
    }
}
