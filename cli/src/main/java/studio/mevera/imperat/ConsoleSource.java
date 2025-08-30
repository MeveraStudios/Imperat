package studio.mevera.imperat;

import studio.mevera.imperat.context.Source;

import java.io.PrintStream;

/**
 * A command-line interface implementation of {@link Source} that wraps a {@link PrintStream}.
 * This class provides a bridge between command-line applications and the Imperat framework,
 * enabling console-based command execution and output.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Simple text-based output to any PrintStream</li>
 *   <li>Support for console applications and CLI tools</li>
 *   <li>Basic message formatting with prefixes for warnings and errors</li>
 *   <li>Always represents a console source (no player distinction)</li>
 * </ul>
 *
 * <p>Usage Example:</p>
 * <pre>{@code
 * CommandLineImperat imperat = CommandLineImperat.builder(System.in)
 *     .build();
 *
 * // The ConsoleSource will automatically use System.out for output
 * }</pre>
 *
 * @since 1.0
 * @author Imperat Framework
 * @see Source
 * @see PrintStream
 */
public class ConsoleSource implements Source {

    private final PrintStream outputStream;

    /**
     * Creates a new ConsoleSource that outputs to the specified PrintStream.
     *
     * @param outputStream the PrintStream to write output to (e.g., System.out)
     */
    public ConsoleSource(final PrintStream outputStream) {
        this.outputStream = outputStream;
    }

    /**
     * Gets the name of this command source.
     * For console sources, this is always "CONSOLE".
     *
     * @return "CONSOLE"
     */
    @Override
    public String name() {
        return "CONSOLE";
    }

    /**
     * Gets the original PrintStream that this ConsoleSource wraps.
     *
     * @return the underlying PrintStream
     */
    @Override
    public PrintStream origin() {
        return outputStream;
    }

    /**
     * Sends a message to the console output stream.
     *
     * @param message the message to send
     */
    @Override
    public void reply(final String message) {
        outputStream.println(message);
    }

    /**
     * Sends a warning message to the console output stream.
     * Warning messages are prefixed with "[WARN]".
     *
     * @param message the warning message to send
     */
    @Override
    public void warn(final String message) {
        outputStream.println("[WARN] " + message);
    }

    /**
     * Sends an error message to the console output stream.
     * Error messages are prefixed with "[ERROR]".
     *
     * @param message the error message to send
     */
    @Override
    public void error(final String message) {
        outputStream.println("[ERROR] " + message);
    }

    /**
     * Checks if this command source is the console.
     * For ConsoleSource, this always returns true.
     *
     * @return true (always, since this is a console source)
     */
    @Override
    public boolean isConsole() {
        return true;
    }

}
