package org.go4j;

public class ChannelException extends RuntimeException {
    protected static String msgInvalid = "invalid channel type";
    protected static String msgClosed = "channel is closed";
    protected static String msgNotReadable = "not allowed to read from channel";
    protected static String msgNotWritable = "not allowed to write to channel";

    public static ChannelException invalid() {
        return new ChannelException(msgInvalid);
    }

    public static ChannelException noReadable() {
        return new ChannelException(msgNotReadable);
    }

    public static ChannelException notWritable() {
        return new ChannelException(msgNotWritable);
    }

    public static ChannelException closed() {
        return new ChannelException(msgClosed);
    }

    public ChannelException(String message) {
        super(message);
    }

    public ChannelException(String message, Throwable cause) {
        super(message, cause);
    }

    public ChannelException(Throwable cause) {
        super(cause);
    }

    protected ChannelException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
