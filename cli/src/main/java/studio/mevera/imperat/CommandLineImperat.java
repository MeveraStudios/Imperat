package studio.mevera.imperat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * Main Imperat implementation for command-line interface applications.
 * This class serves as the primary entry point for integrating the Imperat command framework
 * with console-based applications, providing text-based command execution capabilities.
 *
 * <p>Key Features:</p>
 * <ul>
 *   <li>Simple console-based command execution</li>
 *   <li>Text input/output through standard streams</li>
 *   <li>Lightweight design for CLI applications</li>
 *   <li>No dependency on server platforms</li>
 *   <li>Suitable for standalone tools and utilities</li>
 * </ul>
 *
 * <p>Usage Example:</p>
 * <pre>{@code
 * public class MyCliApp {
 *     public static void main(String[] args) {
 *         CommandLineImperat imperat = CommandLineImperat.builder(System.in)
 *             .build();
 *
 *         imperat.registerCommand(MyCommand.class);
 *
 *         // Process commands from input stream
 *     }
 * }
 * }</pre>
 *
 * @since 1.0
 * @author Imperat Framework
 * @see CommandLineConfigBuilder
 * @see ConsoleCommandSource
 */
public final class CommandLineImperat extends BaseImperat<ConsoleCommandSource> {

    private InputStream input;

    /**
     * Package-private constructor used by CommandLineConfigBuilder.
     * Use {@link #builder(InputStream)} to create instances.
     *
     * @param inputStream the input stream for command reading
     * @param config the Imperat configuration
     */
    CommandLineImperat(InputStream inputStream, ImperatConfig<ConsoleCommandSource> config) {
        super(config);
        this.input = inputStream;
    }

    /**
     * Creates a new configuration builder for CommandLineImperat.
     * This is the recommended way to create and configure a CommandLineImperat instance.
     *
     * @param inputStream the input stream for command line input (e.g., System.in)
     * @return a new CommandLineConfigBuilder instance
     */
    public static CommandLineConfigBuilder builder(InputStream inputStream) {
        return new CommandLineConfigBuilder(inputStream);
    }

    /**
     * Gets the platform object for this implementation.
     * For CLI applications, this returns the input stream.
     *
     * @return the input stream used for command input
     */
    @Override
    public InputStream getPlatform() {
        return input;
    }

    /**
     * Shuts down the platform and releases resources.
     * For CLI applications, this closes the input stream.
     */
    @Override
    public void shutdownPlatform() {
        input = null;
    }

    @Override
    public ConsoleCommandSource createDummySender() {
        return new ConsoleCommandSource(ConsoleLogger.SYSTEM);
    }

    /**
     * Wraps a sender object into a ConsoleCommandSource.
     * For CLI applications, all sources are console sources.
     *
     * @param sender the sender object (typically a ConsoleLogger)
     * @return a new ConsoleCommandSource wrapping the sender
     */
    @Override
    public ConsoleCommandSource wrapSender(Object sender) {
        return new ConsoleCommandSource((ConsoleLogger) sender);
    }

    /**
     * Dispatches the command-line from the input stream provided
     *
     * @param consoleLogger the console logger to write to
     */
    public void execute(ConsoleLogger consoleLogger) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
            String line = reader.readLine();
            ConsoleCommandSource prompt = wrapSender(consoleLogger);
            super.execute(prompt, line);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Dispatches the command-line from the input stream provided
     *
     * @param outputStream the output stream/command-source origin
     * @deprecated Use {@link #execute(ConsoleLogger)} instead
     */
    @Deprecated
    public void execute(OutputStream outputStream) {
        execute(ConsoleLogger.SYSTEM);
    }

    /**
     * Dispatches the command-line from the input stream provided
     * while using {@link ConsoleLogger#SYSTEM} for output.
     */
    public void execute() {
        execute(ConsoleLogger.SYSTEM);
    }
}
