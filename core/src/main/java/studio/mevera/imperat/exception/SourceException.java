package studio.mevera.imperat.exception;

public class SourceException extends CommandException {

    private final String message;
    private final ErrorLevel type;

    public SourceException(
            final String msg,
            final Object... args
    ) {
        super();
        this.type = ErrorLevel.SEVERE;
        this.message = String.format(msg, args);
    }

    public SourceException(
            final ErrorLevel type,
            final String msg,
            final Object... args
    ) {
        super();
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
