package studio.mevera.imperat;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.context.CommandSource;

/**
 * Represents a simple console logger invoked by {@link CommandSource}
 * output methods (e.g. {@code CommandSource#reply}, {@code CommandSource#error}).
 * <p>
 * Applications may implement their preferred solution, such as Java's
 * {@link java.util.logging.Logger} or SLF4J.
 */
public interface ConsoleLogger {

    ConsoleLogger SYSTEM = new SystemConsoleLogger();

    /**
     * Logs an info message.
     *
     * @param message to log as info
     */
    void info(@NotNull String message);

    /**
     * Logs an error message.
     *
     * @param message to log as error
     */
    void error(@NotNull String message);

    /**
     * Logs a warning message.
     * <p>
     * Applications may override this method with a proper warning logger.
     *
     * @param message to log as warning
     */
    default void warn(@NotNull String message) {
        info("WARN: " + message);
    }

    class SystemConsoleLogger implements ConsoleLogger {

        @Override
        public void info(@NotNull String message) {
            System.out.println(message);
        }

        @Override
        public void error(@NotNull String message) {
            System.err.println(message);
        }
    }
}
