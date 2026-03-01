package studio.mevera.imperat;

import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.ThrowableResolver;

/**
 * The {@code ThrowableHandler} interface defines a mechanism for managing and resolving
 * throwable instances within a given context. It provides methods to retrieve specific
 * resolvers for throwable types and handle execution throwable with detailed context
 * information.
 *
 * @param <S> The valueType extending {@link Source} that acts as the source of a command.
 */
public sealed interface ThrowableHandler<S extends Source> permits BaseThrowableHandler {

    /**
     * Registers a {@link ThrowableResolver} for a specific exception type and returns
     * the registered resolver instance. This method allows for fluent configuration
     * of exception handling strategies while providing access to the registered resolver.
     *
     * <p>The registered resolver will be invoked when exceptions of a specified type
     * (or its subtypes, depending on implementation) occur during command execution.
     * This method enables type-safe registration where the exception class and resolver
     * generic types must match.
     *
     * <p><strong>Example usage:</strong>
     * <pre>{@code
     * ThrowableResolver<CommandPermissionException, CommandSource> permissionResolver =
     *     setThrowableResolver(CommandPermissionException.class, (exception, context) -> {
     *         context.source().sendMessage("§cYou don't have permission: " + exception.getPermission());
     *         // Additional logging or cleanup
     *     });
     *
     * // Can be used for method chaining or further configuration
     * permissionResolver.andThen(someOtherAction);
     * }</pre>
     *
     * <p><strong>Type Safety:</strong> The generic constraint ensures that only resolvers
     * capable of handling the specified exception type can be registered, preventing
     * runtime {@link ClassCastException}s during exception resolution.
     *
     * <p><strong>Resolver Precedence:</strong> If a resolver already exists for the given
     * exception type, this method will typically replace it. Refer to the implementation
     * documentation for specific behavior regarding resolver conflicts.
     *
     * <p><strong>Inheritance Behavior:</strong> Depending on the implementation, the
     * resolver may handle subclasses of the specified exception type. Check the
     * concrete implementation's documentation for exact inheritance semantics.
     *
     * @param <T> the specific exception type that the resolver will handle,
     *           must extend {@link Throwable}
     * @param exception the {@link Class} object representing the exception type
     *                 to register a resolver for, must not be {@code null}
     * @param resolver the {@link ThrowableResolver} implementation that will handle
     *                exceptions to the specified type, must not be {@code null}
     *
     * @throws IllegalArgumentException if either {@code exception} or {@code resolver} is {@code null}
     * @throws UnsupportedOperationException if resolver registration is not supported
     *                                      for the given exception type
     *
     * @see ThrowableResolver
     * @see #getThrowableResolver(Class)
     * @since 1.0
     */
    <T extends Throwable> void setThrowableResolver(
            final Class<T> exception,
            final ThrowableResolver<T, S> resolver
    );


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
     * @return Whether the error got handled or not
     */
    <E extends Throwable> boolean handleExecutionThrowable(
            final E throwable,
            final CommandContext<S> context,
            final Class<?> owning,
            final String methodName
    );

}
