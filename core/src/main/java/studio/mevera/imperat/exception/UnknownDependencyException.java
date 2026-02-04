package studio.mevera.imperat.exception;

public final class UnknownDependencyException extends RuntimeException {

    public UnknownDependencyException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnknownDependencyException(String message) {
        super(message);
    }
}
