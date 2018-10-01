package uk.org.binky.jchan;

public class ChannelAlreadyClosedException extends RuntimeException {
    public ChannelAlreadyClosedException(final String msg) {
        super(msg);
    }
}
