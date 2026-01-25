package studio.mevera.imperat;

import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Strategy interface responsible for printing throwables that are
 * caught but not otherwise handled.
 *
 * <p>This allows platforms to customise how unhandled exceptions are
 * presented, whether by logging frameworks, plain printing, or other
 * mechanisms.</p>
 */
@FunctionalInterface
public interface ThrowablePrinter {

    /**
     * Prints the stacktrace for the given throwable.
     *
     * @param throwable the throwable to print
     */
    void print(@NotNull Throwable throwable);

    /**
     * Returns a simple printer that logs the throwable using the
     * {@code IMPERAT} logger.
     *
     * @return a basic throwable printer
     */
    static @NotNull ThrowablePrinter simple() {
        return throwable -> Logger.getLogger("IMPERAT")
            .log(Level.SEVERE, "Unhandled exception", throwable);
    }

    /**
     * Returns a printer that formats the throwable into a decorative box
     * and logs it using the {@code IMPERAT} logger.
     *
     * @return a boxed throwable printer
     */
    static @NotNull ThrowablePrinter box() {
        return throwable -> Logger.getLogger("IMPERAT")
            .severe(formatThrowable(throwable));
    }

    private static @NotNull String formatThrowable(@NotNull Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        String exceptionName = throwable.getClass().getSimpleName();
        String message = throwable.getMessage() != null ? throwable.getMessage() : "No message";

        sb.append("┌─ 🚨 ").append(exceptionName).append(' ')
            .append("─".repeat(Math.max(0, 40 - exceptionName.length()))).append("┐");
        sb.append(System.lineSeparator()).append("│ ").append(message);

        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append(System.lineSeparator()).append("│ → ")
                .append(element.getClassName().substring(element.getClassName().lastIndexOf('.') + 1))
                .append('.').append(element.getMethodName())
                .append("() @ line ").append(element.getLineNumber());
        }

        sb.append(System.lineSeparator()).append("└").append("─".repeat(45)).append("┘");

        if (throwable.getCause() != null) {
            sb.append(System.lineSeparator()).append("🔗 Caused by:")
                .append(System.lineSeparator())
                .append(formatThrowable(throwable.getCause()));
        }

        return sb.toString();
    }
}
