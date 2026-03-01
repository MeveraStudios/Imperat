package studio.mevera.imperat.exception;

import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.Source;

/**
 * A functional interface for handling and resolving exceptions that occur during
 * Imperat's command execution flow. Implementations of this interface define
 * custom error handling strategies for specific exception types and source contexts.
 *
 * <p>This resolver pattern allows for flexible exception handling where different
 * types of exceptions can be handled differently based on their type and the
 * context in which they occurred. For example, permission-related exceptions
 * might be handled differently from syntax errors or internal command failures.
 *
 * <p>Example usage:
 * <pre>{@code
 * ThrowableResolver<CommandSyntaxException, CommandSource> syntaxResolver =
 *     (exception, context) -> {
 *         context.source().sendMessage("Invalid command syntax: " + exception.getMessage());
 *         // Log the error or perform additional cleanup
 *     };
 * }</pre>
 *
 * @param <E> the specific type of exception this resolver can handle, must extend {@link Throwable}
 * @param <S> the type of source that provides context for the command execution, must extend {@link Source}
 *
 * @since 1.0
 * @see CommandContext
 * @see Source
 * @author Imperat Framework
 */
@FunctionalInterface
public interface ThrowableResolver<E extends Throwable, S extends Source> {

    /**
     * Resolves the given exception within the provided execution context.
     * This method is called when an exception of type {@code E} occurs during
     * command execution, allowing for custom handling such as user notification,
     * logging, cleanup operations, or alternative command flows.
     *
     * <p>Implementations should be mindful of:
     * <ul>
     *   <li>Not throwing additional exceptions unless absolutely necessary</li>
     *   <li>Providing meaningful feedback to the command source when appropriate</li>
     *   <li>Performing any necessary cleanup or state restoration</li>
     *   <li>Logging errors for debugging purposes when needed</li>
     * </ul>
     *
     * <p>The resolution process should be non-blocking and efficient, as it's
     * part of the critical command execution path.
     *
     * @param exception the exception that occurred during command execution, never {@code null}
     * @param context the execution context containing the source, command state, and other
     *                relevant information at the time the exception occurred, never {@code null}
     *
     * @throws RuntimeException if the resolution process itself fails critically
     *                         (though implementations should avoid this when possible)
     *
     * @see CommandContext#source()
     * @see CommandContext#command()
     */
    void resolve(final E exception, CommandContext<S> context);

}
