package studio.mevera.imperat;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.SelfHandledException;
import studio.mevera.imperat.exception.ThrowableResolver;
import studio.mevera.imperat.util.ImperatDebugger;

/**
 * A base implementation of {@link ThrowableHandler} that provides intelligent
 * exception chain traversal and resolution logic. This interface serves as a
 * foundation for command execution error handling in the Imperat framework.
 *
 * <p>This handler implements a comprehensive exception resolution strategy that:
 * <ol>
 *   <li>Traverses the entire exception cause chain from the root exception to the deepest cause</li>
 *   <li>Prioritizes {@link SelfHandledException} instances for immediate self-resolution</li>
 *   <li>Falls back to registered {@link ThrowableResolver} instances for standard exceptions</li>
 *   <li>Provides detailed debugging information throughout the resolution process</li>
 *   <li>Logs unhandled exceptions for debugging purposes</li>
 * </ol>
 *
 * <p>The {@code non-sealed} modifier allows for further extension while maintaining
 * the base exception handling contract.
 *
 * <p><strong>Exception Resolution Priority:</strong>
 * <pre>{@code
 * 1. SelfHandledException.handle() - immediate self-resolution
 * 2. Registered ThrowableResolver for the specific exception type
 * 3. Traverse to exception.getCause() and repeat
 * 4. Log error if no handler found in the entire chain
 * }</pre>
 *
 * <p><strong>Example implementation:</strong>
 * <pre>{@code
 * public class CommandExceptionHandler implements BaseThrowableHandler<CommandSource> {
 *
 *     @Override
 *     public ThrowableResolver<?, CommandSource> getThrowableResolver(Class<?> exceptionType) {
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
 * @see ThrowableResolver
 * @see SelfHandledException
 * @see Context
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
     *       {@link SelfHandledException#handle(ImperatConfig, Context)} when encountered</li>
     *   <li><strong>Resolver Lookup:</strong> Searches for registered {@link ThrowableResolver}
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
     *     // Command execution
     * } catch (CommandPermissionException e) {
     *     // 1. Check if CommandPermissionException is SelfHandledException → No
     *     // 2. Look for ThrowableResolver<CommandPermissionException> → Found
     *     // 3. Call resolver.resolve(e, context) → Success, return
     * } catch (WrappedException e) {
     *     // 1. Check WrappedException → No resolver
     *     // 2. Check e.getCause() (CommandSyntaxException) → Found resolver
     *     // 3. Resolve and return
     * }
     * }</pre>
     *
     * <p><strong>Thread Safety:</strong> This method is thread-safe assuming the
     * underlying {@link #getThrowableResolver(Class)} implementation is thread-safe.
     *
     * @param throwable  the root exception that occurred during command execution,
     *                   may be {@code null} (though this typically indicates a logic error)
     * @param context    the execution context containing the command source, configuration,
     *                   and other relevant state information, never {@code null}
     * @param owning     the class where the exception originated, used for debugging
     *                   and error logging purposes, may be {@code null}
     * @param methodName the name of the method where the exception occurred,
     *                   used for debugging and error logging, may be {@code null}
     * @return whether the exception got handled or not.
     * @implNote The default implementation never throws exceptions itself, ensuring
     * that exception handling does not create additional execution failures.
     * All resolver calls are wrapped in implicit exception safety.
     * @see #getThrowableResolver(Class)
     * @see SelfHandledException#handle(ImperatConfig, Context)
     * @see ImperatDebugger#debug(String, Object...)
     * @see ImperatDebugger#error(Class, String, Throwable)
     */
    @Override
    @SuppressWarnings("unchecked")
    default <E extends Throwable> boolean handleExecutionThrowable(
            @NotNull E throwable,
            Context<S> context,
            Class<?> owning,
            String methodName
    ) {
        
        Throwable current = throwable;
        
        while (current != null) {
            if (current instanceof SelfHandledException selfHandledException) {
                selfHandledException.handle(context.imperatConfig(), context);
                return true;
            }
            
            ThrowableResolver<? super Throwable, S> handler = (ThrowableResolver<? super Throwable, S>) this.getThrowableResolver(current.getClass());
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