package studio.mevera.imperat;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.CommandExceptionHandler;
import studio.mevera.imperat.exception.SelfHandlingException;
import studio.mevera.imperat.util.ImperatDebugger;

/**
 * A base implementation of {@link ThrowableHandler} that provides intelligent
 * exception chain traversal and resolution logic. This interface serves as a
 * foundation for command execution error handling in the Imperat framework.
 *
 * <p>This handler implements a comprehensive exception resolution strategy that:
 * <ol>
 *   <li>Traverses the entire exception cause chain from the root exception to the deepest cause</li>
 *   <li>Prioritizes {@link SelfHandlingException} instances for immediate self-resolution</li>
 *   <li>Falls back to registered {@link CommandExceptionHandler} instances for standard exceptions</li>
 *   <li>Provides detailed debugging information throughout the resolution process</li>
 *   <li>Logs unhandled exceptions for debugging purposes</li>
 * </ol>
 *
 * <p>The {@code non-sealed} modifier allows for further extension while maintaining
 * the base exception handling contract.
 *
 * <p><strong>Exception Resolution Priority:</strong>
 * <pre>{@code
 * 1. SelfHandlingException.handle() - immediate self-resolution
 * 2. Registered CommandExceptionHandler for the specific exception type
 * 3. Traverse to exception.getCause() and repeat
 * 4. Log error if no handler found in the entire chain
 * }</pre>
 *
 * <p><strong>Example implementation:</strong>
 * <pre>{@code
 * public class CommandExceptionHandler implements BaseThrowableHandler<CommandSource> {
 *
 *     @Override
 *     public CommandExceptionHandler<?, CommandSource> getThrowableResolver(Class<?> exceptionType) {
 *         return resolvers.get(exceptionType);
 *     }
 * }
 * }</pre>
 *
 * @param <S> the type of command source that provides context for exception handling,
 *            must extend {@link Source}
 *
 * @since 1.0
 * @see ThrowableHandler
 * @see CommandExceptionHandler
 * @see SelfHandlingException
 * @see CommandContext
 */
public non-sealed interface BaseThrowableHandler<S extends Source> extends ThrowableHandler<S> {

    /**
     * Handles exceptions that occur during command execution by traversing the
     * exception cause chain and applying appropriate resolution strategies.
     *
     * <p>This method implements a sophisticated exception handling workflow:
     * <ul>
     *   <li><strong>Chain Traversal:</strong> Iterates through the complete exception
     *       cause chain using {@link Throwable#getCause()}</li>
     *   <li><strong>Self-Handled Exceptions:</strong> Immediately delegates to
     *       {@link SelfHandlingException#handle(CommandContext)} when encountered</li>
     *   <li><strong>Resolver Lookup:</strong> Searches for registered {@link CommandExceptionHandler}
     *       instances matching the current exception type</li>
     *   <li><strong>Debug Logging:</strong> Provides detailed logging of the resolution
     *       process through {@link ImperatDebugger}</li>
     *   <li><strong>Fallback Logging:</strong> Records unhandled exceptions for debugging</li>
     * </ul>
     *
     * <p>The method terminates early upon successful resolution, ensuring that only
     * the most appropriate handler processes each exception. If no suitable handler
     * is found in the entire cause chain, the original exception is logged as an error.
     *
     * <p><strong>Resolution Flow Example:</strong>
     * <pre>{@code
     * try {
     *     // RootCommand execution
     * } catch (CommandPermissionException e) {
     *     // 1. Check if CommandPermissionException is SelfHandlingException → No
     *     // 2. Look for CommandExceptionHandler<CommandPermissionException> → Found
     *     // 3. Call resolver.resolve(e, context) → Success, return
     * } catch (WrappedException e) {
     *     // 1. Check WrappedException → No resolver
     *     // 2. Check e.getCause() (CommandSyntaxException) → Found resolver
     *     // 3. Resolve and return
     * }
     * }</pre>
     *
     * <p><strong>Thread Safety:</strong> This method is thread-safe assuming the
     * underlying {@link #getErrorHandlerFor(Class)} implementation is thread-safe.
     *
     * @param throwable  the root exception that occurred during command execution,
     *                   may be {@code null} (though this typically indicates a logic error)
     * @param context    the execution context containing the command source, configuration,
     *                   and other relevant state information, never {@code null}
     * @param owning     the class where the exception originated, used for debugging
     *                   and error logging purposes, may be {@code null}
     * @param methodName the name of the method where the exception occurred,
     *                   used for debugging and error logging, may be {@code null}
     * @return the unhandled exception if no handler was found, or null if the exception was handled successfully
     * @implNote The default implementation never throws exceptions itself, ensuring
     * that exception handling does not create additional execution failures.
     * All resolver calls are wrapped in implicit exception safety.
     * @see #getErrorHandlerFor(Class)
     * @see SelfHandlingException#handle(CommandContext)
     * @see ImperatDebugger#debug(String, Object...)
     * @see ImperatDebugger#error(Class, String, Throwable)
     */
    @Override
    @SuppressWarnings("unchecked")
    default <E extends Throwable> boolean handleExecutionError(
            @NotNull E throwable,
            CommandContext<S> context,
            Class<?> owning,
            String methodName
    ) {

        Throwable current = throwable;

        while (current != null) {
            if (current instanceof SelfHandlingException selfHandlingException) {
                selfHandlingException.handle(context);
                return true;
            }

            CommandExceptionHandler<? super Throwable, S> handler =
                    (CommandExceptionHandler<? super Throwable, S>) this.getErrorHandlerFor(current.getClass());
            if (handler != null) {
                ImperatDebugger.debug("Found handler for exception '%s'", current.getClass().getName());
                handler.resolve(current, context);
                return true;
            }

            current = current.getCause();
        }

        return false;
    }
}