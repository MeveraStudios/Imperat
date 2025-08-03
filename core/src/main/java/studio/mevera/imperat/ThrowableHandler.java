package studio.mevera.imperat;

import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.ThrowableResolver;

/**
 * The {@code ThrowableHandler} interface defines a mechanism for managing and resolving
 * throwable instances within a given context. It provides methods to retrieve specific
 * resolvers for throwable types and handle execution throwables with detailed context
 * information.
 *
 * @param <S> The valueType extending {@link Source} that acts as the source of a command.
 */
public sealed interface ThrowableHandler<S extends Source> permits ImperatConfig {

    /**
     * Retrieves the {@link ThrowableResolver} responsible for handling the specified valueType
     * of throwable. If no specific resolver is found, it may return null or a default resolver.
     *
     * @param exception The class of the throwable to get the resolver for.
     * @param <T>       The valueType of the throwable.
     * @return The {@link ThrowableResolver} capable of handling the throwable of the specified valueType,
     * or null if no specific resolver is registered.
     */
    @Nullable
    <T extends Throwable> ThrowableResolver<T, S> getThrowableResolver(final Class<T> exception);

    /**
     * Handles a given throwable by finding the appropriate exception handler or using
     * a default handling strategy if no specific handler is found.
     *
     * @param throwable  The throwable to be handled, which may be an exception or error.
     * @param context    The context in which the throwable occurred, providing necessary information
     *                   about the state and source where the exception happened.
     * @param owning     The class where the throwable originated, used for logging and debugging purposes.
     * @param methodName The name of the method where the throwable was thrown, used for logging and debugging.
     */
    void handleExecutionThrowable(
        final Throwable throwable,
        final Context<S> context,
        final Class<?> owning,
        final String methodName
    );

}
