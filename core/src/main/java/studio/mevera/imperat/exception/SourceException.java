package studio.mevera.imperat.exception;

import studio.mevera.imperat.context.Context;

public class SourceException extends ImperatException {

    private final String message;
    private final ErrorLevel type;
    public SourceException(
        Context<?> ctx,
        final String msg,
        final Object... args
    ) {
        super(ctx);
        this.type = ErrorLevel.SEVERE;
        this.message = String.format(msg, args);
    }

    public SourceException(
        Context<?> ctx,
        final ErrorLevel type,
        final String msg,
        final Object... args
    ) {
        super(ctx);
        this.type = type;
        this.message = String.format(msg, args);
    }

    public ErrorLevel getType() {
        return type;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    public enum ErrorLevel {
        REPLY,
        WARN,
        SEVERE
    }

}
