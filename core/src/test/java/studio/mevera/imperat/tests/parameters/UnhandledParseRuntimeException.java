package studio.mevera.imperat.tests.parameters;

/**
 * Runtime exception type that is intentionally NOT registered with any exception handler
 * (neither command-local nor global). Used to regression-test that parse failures with
 * no registered handler fall back to {@code InvalidSyntaxException} instead of being
 * propagated raw.
 */
public final class UnhandledParseRuntimeException extends RuntimeException {
    public UnhandledParseRuntimeException(String message) {
        super(message);
    }
}
