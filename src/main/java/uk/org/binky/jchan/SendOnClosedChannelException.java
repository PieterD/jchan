package uk.org.binky.jchan;

public class SendOnClosedChannelException extends RuntimeException {
    public SendOnClosedChannelException(final String msg) {
        super(msg);
    }
}
