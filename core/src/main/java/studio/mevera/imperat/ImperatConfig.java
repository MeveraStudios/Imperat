package studio.mevera.imperat;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.annotations.base.AnnotationReplacer;
import studio.mevera.imperat.annotations.base.InstanceFactory;
import studio.mevera.imperat.annotations.base.element.ParameterElement;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandCoordinator;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.ContextArgumentProviderFactory;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.command.arguments.type.Cursor;
import studio.mevera.imperat.command.suggestions.AutoCompleterFactory;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.internal.ContextFactory;
import studio.mevera.imperat.events.EventBus;
import studio.mevera.imperat.exception.CommandExceptionHandler;
import studio.mevera.imperat.permissions.PermissionChecker;
import studio.mevera.imperat.placeholders.Placeholder;
import studio.mevera.imperat.placeholders.PlaceholderResolver;
import studio.mevera.imperat.providers.CommandSourceMapper;
import studio.mevera.imperat.providers.ContextArgumentProvider;
import studio.mevera.imperat.providers.DependencySupplier;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Optional;

/**
 * The {@code ImperatConfig} interface defines the core configuration and interaction points
 * for a command processing system. It provides methods for registering handlers,
 * resolving dependencies, and managing the execution context. This interface enables
 * integration of custom resolvers, context factories, permission management, and
 * placeholder handling.
 * <p>
 * Implementations of this interface are responsible for providing these required
 * functionalities and may enforce strict validation and processing rules based on
 * the specified configurations.
 * </p>
 *
 * @param <S> The type of the source object used by the configuration, which implements the {@link CommandSource} interface.
 */
public sealed interface ImperatConfig<S extends CommandSource> extends ResolverRegistrar<S>, BaseThrowableHandler<S>
        permits ImperatConfigImpl {

    /**
     * @return The command prefix
     */
    String commandPrefix();

    void setCommandPrefix(String cmdPrefix);

    /**
     * Class token for the canonical source type {@code S}. Captured at
     * builder-construction time. Required by reflective param-resolution
     * paths that compare {@code @Execute} method parameters against the
     * user-declared source class.
     */
    @NotNull Class<S> sourceClass();

    /**
     * Bidirectional mapper between the platform-native source and the
     * canonical {@code S}. Default-path builders install
     * {@link CommandSourceMapper#identity()}; custom-source plugins
     * install their own via {@code .source(...)}.
     *
     * <p>Returned as a raw {@link CommandSourceMapper} because the
     * {@code S extends P} bound on the interface is incompatible with
     * wildcard storage across generic erasure. Callers cast to their
     * expected platform type.</p>
     */
    @SuppressWarnings("rawtypes")
    @NotNull CommandSourceMapper sourceMapper();

    /**
     * Sets the source mapper. Called by the platform-specific
     * {@code ConfigBuilder} when the user chains {@code .source(...)}.
     */
    @SuppressWarnings("rawtypes")
    void setSourceMapper(@NotNull CommandSourceMapper mapper);

    /**
     * @return the printer used for unhandled throwables
     */
    @NotNull ThrowablePrinter getThrowablePrinter();

    /**
     * Sets the printer used for unhandled throwables.
     *
     * @param printer the throwable printer to use
     */
    void setThrowablePrinter(@NotNull ThrowablePrinter printer);

    /**
     * Fetches {@link ArgumentType} for a certain value
     *
     * @param resolvingValueType the value that the resolver ends providing it from the context
     * @return the value resolver of a certain valueType
     */
    @Nullable
    ArgumentType<S, ?> getArgumentType(Type resolvingValueType);

    default boolean hasArgumentType(Type type) {
        return getArgumentType(type) != null;
    }

    /**
     * Registers an {@link AnnotationReplacer} that will be applied to the
     * annotation parser when the owning {@link Imperat} instance is built.
     *
     * <p>The replacer is staged on this config; the framework forwards it to
     * the parser internally during {@code Imperat} construction. Callers do
     * not need to (and cannot) invoke any "apply" step themselves.</p>
     *
     * @param type the type of annotation to register
     * @param replacer the replacer for this annotation
     * @param <A> the type of annotation to replace by the {@link AnnotationReplacer}
     */
    <A extends Annotation> void registerAnnotationReplacer(Class<A> type, AnnotationReplacer<A> replacer);

    /**
     * Determines whether multiple optional parameters can be suggested simultaneously
     * during tab completion at the same command depth level.
     *
     * <p>When enabled ({@code true}), all available optional parameters will be included
     * in tab completion suggestions, allowing users to see all possible optional arguments
     * they can provide at the current position.
     *
     * <p>When disabled ({@code false}), only the first optional parameter (typically based
     * on priority or registration order) will be suggested, preventing overwhelming users
     * with too many optional choices and reducing ambiguity in command completion.
     *
     * <p>This setting does not affect:
     * <ul>
     *   <li>Required parameters - they are always suggested</li>
     *   <li>RootCommand structure - the actual command tree remains unchanged</li>
     *   <li>Parameter validation - all parameters remain functionally available</li>
     * </ul>
     *
     * @return {@code true} if multiple optional parameters can overlap in suggestions,
     *         {@code false} if only one optional parameter should be suggested at a time
     * @see #setOptionalParameterSuggestionOverlap(boolean)
     */
    boolean isOptionalParameterSuggestionOverlappingEnabled();

    /**
     * Sets whether multiple optional parameters can be suggested simultaneously
     * during tab completion at the same command depth level.
     *
     * <p>This is a configuration setting that affects the behavior of tab completion
     * suggestions without modifying the underlying command structure. The command
     * tree and parameter validation remain unchanged regardless of this setting.
     *
     * <p><strong>Examples:</strong>
     * <pre>{@code
     * // RootCommand structure: /command [count] [extra]
     * //                              \[extra]
     *
     * // When enabled (true):
     * /command <TAB> → shows: [count], [extra]
     *
     * // When disabled (false):
     * /command <TAB> → shows: [count] (first optional only)
     * }</pre>
     *
     * @param enabled {@code true} to allow multiple optional parameter suggestions,
     *                {@code false} to limit to one optional parameter suggestion
     * @see #isOptionalParameterSuggestionOverlappingEnabled()
     */
    void setOptionalParameterSuggestionOverlap(boolean enabled);

    /**
     * Checks whether the valueType has
     * a registered context-resolver
     *
     * @param type the valueType
     * @return whether the valueType has
     * a context-resolver
     */
    boolean hasContextResolver(Type type);

    /**
     * Fetches {@link ContextArgumentProvider} for a certain valueType
     *
     * @param resolvingContextType the valueType for this resolver
     * @param <T>                  the valueType of class
     * @return the context resolver
     */
    @Nullable
    <T> ContextArgumentProvider<S, T> getContextArgumentProvider(Type resolvingContextType);

    /**
     * Fetches the context resolver for {@link ParameterElement} of a method
     *
     * @param element the element
     * @param <T>     the valueType of value this parameter should be resolved into
     * @return the {@link ContextArgumentProvider} for this element
     */
    @Nullable
    <T> ContextArgumentProvider<S, T> getContextArgumentProviderFor(@NotNull ParameterElement element);

    /**
     * Fetches the {@link ContextArgumentProvider} suitable for the {@link Argument}
     *
     * @param Argument the parameter of a command's usage
     * @param <T>              the valueType of value that will be resolved by
     * {@link ArgumentType#parse(CommandContext, Argument, Cursor)} during execution
     * @return the context resolver for this parameter's value valueType
     */
    default <T> ContextArgumentProvider<S, T> getContextArgumentProvider(Argument<S> Argument) {
        return getContextArgumentProvider(Argument.valueType());
    }

    /**
     * @param resolvingContextType the valueType the factory is registered to
     * @return returns the factory for creation of
     * {@link ContextArgumentProvider}
     */
    @Nullable
    <T> ContextArgumentProviderFactory<S, T> getContextArgumentProviderFactory(Type resolvingContextType);

    /**
     * Determines whether strict ambiguity validation is enabled for command registration.
     *
     * <p>When enabled ({@code true}), Imperat performs aggressive ambiguity checks
     * during command tree construction and registration. Commands that contain
     * potentially conflicting parsing pathways will fail registration with an
     * {@link studio.mevera.imperat.exception.AmbiguousCommandException}.
     *
     * <p>This includes validation such as:
     * <ul>
     *   <li>Sibling argument conflicts with overlapping priorities or value types</li>
     *   <li>Optional parameter pathway overlaps</li>
     *   <li>Greedy arguments followed by additional parameters</li>
     *   <li>Conflicting optional parsing branches</li>
     * </ul>
     *
     * <p>When disabled ({@code false}), Imperat skips strict ambiguity validation,
     * allowing potentially overlapping command pathways to coexist. This can be
     * useful for experimental parsers, advanced custom argument resolvers, or
     * intentionally flexible command structures.
     *
     * <p><strong>Warning:</strong> Disabling strict ambiguity resolution may result
     * in undefined parsing behavior if multiple arguments can consume the same input
     * token sequence.
     *
     * <p>This setting only affects registration-time validation and does not change
     * the runtime parser implementation itself.
     *
     * <p>Enabled by default.
     *
     * @return {@code true} if strict ambiguity validation is enabled,
     *         {@code false} otherwise
     * @see #setStrictAmbiguityResolution(boolean)
     * @see studio.mevera.imperat.exception.AmbiguousCommandException
     */
    boolean isStrictAmbiguityResolutionEnabled();

    /**
     * Enables or disables strict ambiguity validation during command registration.
     *
     * <p>Strict ambiguity resolution ensures that command pathways remain
     * deterministic and free from overlapping parsing patterns. When enabled,
     * Imperat validates command structures and rejects ambiguous pathways before
     * they can be registered.
     *
     * <p>Disabling this setting allows more permissive command trees and may be
     * useful when:
     * <ul>
     *   <li>Using custom argument types with non-standard parsing behavior</li>
     *   <li>Experimenting with overlapping optional parameters</li>
     *   <li>Building advanced fallback parsing systems</li>
     *   <li>Temporarily bypassing validation during development</li>
     * </ul>
     *
     * <p><strong>Example:</strong>
     * <pre>{@code
     * config.setStrictAmbiguityResolution(false);
     *
     * // Potentially overlapping pathways become allowed:
     * // /command <player>
     * // /command <offline-player>
     * }</pre>
     *
     * <p><strong>Warning:</strong> Disabling strict ambiguity validation may cause
     * commands to parse unpredictably if multiple arguments match the same input.
     * It is recommended to keep this enabled unless the command structure is fully
     * controlled and intentionally designed for overlap.
     *
     * <p>Enabled by default.
     *
     * @param enabled {@code true} to enforce strict ambiguity validation,
     *                {@code false} to allow potentially ambiguous pathways
     * @return this config instance
     * @see #isStrictAmbiguityResolutionEnabled()
     * @see studio.mevera.imperat.exception.AmbiguousCommandException
     */
    ImperatConfig<S> setStrictAmbiguityResolution(boolean enabled);

    /**
     * @return {@link PermissionChecker} for the dispatcher
     */
    PermissionChecker<S> getPermissionChecker();

    /**
     * Sets the permission resolver for the platform
     *
     * @param permissionChecker the permission resolver to set
     */
    void setPermissionResolver(PermissionChecker<S> permissionChecker);

    /**
     * @return the factory for creation of
     * command related contexts {@link CommandContext}
     */
    ContextFactory<S> getContextFactory();

    /**
     * sets the context factory {@link ContextFactory} for the contexts
     *
     * @param contextFactory the context factory to set
     */
    void setContextFactory(ContextFactory<S> contextFactory);

    /**
     * The id/format of this placeholder, must be unique and lowercase
     *
     * @param id the id for the placeholder
     * @return the placeholder
     */
    Optional<Placeholder> getPlaceHolder(String id);

    /**
     * Replaces the placeholders of input by their {@link PlaceholderResolver}
     *
     * @param input the input
     * @return the processed/replaced text input.
     */
    @NotNull
    String replacePlaceholders(String input);

    /**
     * Replaces the placeholders on each string of the array,
     * modifying the input array content.
     *
     * @param array the array to replace its string contents
     * @return The placeholder replaced String array
     */
    @NotNull
    String[] replacePlaceholders(String[] array);

    /**
     * Registers the dependency to the type
     *
     * @param type     the type for the dependency
     * @param resolver the resolver
     */
    void registerDependencyResolver(Type type, DependencySupplier resolver);

    /**
     * Resolves dependency of certain type
     *
     * @param type the type
     */
    <T> @Nullable T resolveDependency(Type type);

    /**
     * Registers a new {@link CommandExceptionHandler} for the specified valueType of throwable.
     * This allows customizing the handling of specific throwable types within the application.
     *
     * @param exception The class of the throwable to set the resolver for.
     * @param handler   The {@link CommandExceptionHandler} to be registered for the specified throwable valueType.
     * @param <T>       The valueType of the throwable.
     */
    <T extends Throwable> void setErrorHandler(
            final Class<T> exception,
            final CommandExceptionHandler<T, S> handler
    );

    /**
     * @return The global/centralized default usage of EVERY command
     * its empty by default.
     */
    @NotNull CommandPathway.Builder<S> getGlobalDefaultPathway();

    /**
     * Sets the usual default usage if the user doesn't set
     * the default-usage for a {@link Command}
     * @param globalDefaultUsage the global default usage BUILDER.
     */
    void setGlobalDefaultPathway(@NotNull CommandPathway.Builder<S> globalDefaultUsage);

    /**
     * The factory for creating instances of types to be dependency injected.
     * @return the instance factory
     */
    InstanceFactory<S> getInstanceFactory();

    /**
     * Sets the instance factory for creating instances of types to be dependency injected.
     * @param factory the instance factory to set
     */
    void setInstanceFactory(InstanceFactory<S> factory);

    /**
     * @return the factory used to create an {@link studio.mevera.imperat.command.suggestions.AutoCompleter}
     * for each command
     */
    AutoCompleterFactory<S> getAutoCompleterFactory();

    /**
     * Sets the factory used to create an {@link studio.mevera.imperat.command.suggestions.AutoCompleter}
     * for each command.
     *
     * @param factory the factory to set
     */
    void setAutoCompleterFactory(AutoCompleterFactory<S> factory);

    /**
     * @return the default global command coordinator
     */
    CommandCoordinator<S> getGlobalCommandCoordinator();

    void setGlobalCommandCoordinator(CommandCoordinator<S> commandCoordinator);

    void setCoroutineScope(@NotNull Object scope);

    @Nullable Object getCoroutineScope();

    boolean hasCoroutineScope();

    ImperatConfig<S> setCommandParsingMode(CommandParsingMode mode);

    CommandParsingMode getCommandParsingMode();

    EventBus getEventBus();

    void setEventBus(EventBus eventBus);
}
