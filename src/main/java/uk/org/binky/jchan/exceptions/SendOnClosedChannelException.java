package uk.org.binky.jchan.exceptions;

public class SendOnClosedChannelException extends RuntimeException {
    public SendOnClosedChannelException(final String msg) {
        super(msg);
    }
}
