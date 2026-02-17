package studio.mevera.imperat;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.annotations.base.AnnotationReplacer;
import studio.mevera.imperat.annotations.base.InstanceFactory;
import studio.mevera.imperat.annotations.base.element.ParameterElement;
import studio.mevera.imperat.command.AttachmentMode;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandCoordinator;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.command.ContextResolverFactory;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.command.parameters.type.ArgumentType;
import studio.mevera.imperat.command.tree.help.HelpCoordinator;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.context.internal.ContextFactory;
import studio.mevera.imperat.context.internal.flow.ParameterValueAssigner;
import studio.mevera.imperat.events.EventBus;
import studio.mevera.imperat.exception.ThrowableResolver;
import studio.mevera.imperat.permissions.PermissionChecker;
import studio.mevera.imperat.placeholders.Placeholder;
import studio.mevera.imperat.placeholders.PlaceholderResolver;
import studio.mevera.imperat.resolvers.ContextResolver;
import studio.mevera.imperat.resolvers.DependencySupplier;

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
 * @param <S> The type of the source object used by the configuration, which implements the {@link Source} interface.
 */
public sealed interface ImperatConfig<S extends Source> extends
        ProcessorRegistrar<S>, ResolverRegistrar<S>, BaseThrowableHandler<S>
        permits ImperatConfigImpl {

    /**
     * @return The command prefix
     */
    String commandPrefix();

    void setCommandPrefix(String cmdPrefix);

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
     * Registers annotation replacer
     * @param type the type of annotation to register
     * @param replacer the replacer for this annotation
     * @param <A> the type of annotation to replace by the {@link AnnotationReplacer}
     */
    <A extends Annotation> void registerAnnotationReplacer(Class<A> type, AnnotationReplacer<A> replacer);

    /**
     * Apply annotation replacers.
     * @param imperat the imperat instance
     * @param <A> A type variable used internally
     */
    @ApiStatus.Internal
    <A extends Annotation> void applyAnnotationReplacers(Imperat<S> imperat);

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
     *   <li>Command structure - the actual command tree remains unchanged</li>
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
     * // Command structure: /command [count] [extra]
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
     * <p>
     * Whether to handle the skipping of consecutive optional argument <b>during execution</b>
     * For example if you have `/test [a] [b]` where parameter 'a' is of type String
     * and parameter 'b' is of type Integer.
     * if you enter `/test 1` while this option is enabled, it would handle this and assign
     * the parameter 'b' to the value that suits its type.
     * with no respect for the order of optional arguments.
     * <p>
     * Else if the option is disabled, then Imperat's {@link ParameterValueAssigner}
     * will respect the order of the optional arguments, and will resolve the arguments in order.
     *
     *
     * @return Whether to handle the skipping of consecutive optional argument
     * <b>DURING EXECUTION</b>.
     */
    boolean handleExecutionMiddleOptionalSkipping();

    /**
     * Refer to {@link #handleExecutionMiddleOptionalSkipping()} to know about this option.
     * @param toggle whether to toggle the handling of middle optional skipping
     */
    void setHandleExecutionConsecutiveOptionalArgumentsSkip(boolean toggle);

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
     * Fetches {@link ContextResolver} for a certain valueType
     *
     * @param resolvingContextType the valueType for this resolver
     * @param <T>                  the valueType of class
     * @return the context resolver
     */
    @Nullable
    <T> ContextResolver<S, T> getContextResolver(Type resolvingContextType);

    /**
     * Fetches the context resolver for {@link ParameterElement} of a method
     *
     * @param element the element
     * @param <T>     the valueType of value this parameter should be resolved into
     * @return the {@link ContextResolver} for this element
     */
    @Nullable
    <T> ContextResolver<S, T> getMethodParamContextResolver(@NotNull ParameterElement element);

    /**
     * Fetches the {@link ContextResolver} suitable for the {@link Argument}
     *
     * @param Argument the parameter of a command's usage
     * @param <T>              the valueType of value that will be resolved by
     * {@link ArgumentType#parse(ExecutionContext, Cursor, String)}
     * @return the context resolver for this parameter's value valueType
     */
    default <T> ContextResolver<S, T> getContextResolver(Argument<S> Argument) {
        return getContextResolver(Argument.valueType());
    }

    /**
     * @param resolvingContextType the valueType the factory is registered to
     * @return returns the factory for creation of
     * {@link ContextResolver}
     */
    @Nullable
    <T> ContextResolverFactory<S, T> getContextResolverFactory(Type resolvingContextType);

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
     * command related contexts {@link Context}
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
    Optional<Placeholder<S>> getPlaceHolder(String id);

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

    default boolean hasSourceResolver(Type wrap) {
        return getSourceResolver(wrap) != null;
    }

    /**
     * Registers a new {@link ThrowableResolver} for the specified valueType of throwable.
     * This allows customizing the handling of specific throwable types within the application.
     *
     * @param exception The class of the throwable to set the resolver for.
     * @param handler   The {@link ThrowableResolver} to be registered for the specified throwable valueType.
     * @param <T>       The valueType of the throwable.
     */
    <T extends Throwable> void setThrowableResolver(
            final Class<T> exception,
            final ThrowableResolver<T, S> handler
    );

    /**
     * @return The global/centralized default usage of EVERY command
     * its empty by default.
     */
    @NotNull CommandUsage.Builder<S> getGlobalDefaultUsage();

    /**
     * Sets the usual default usage if the user doesn't set
     * the default-usage for a {@link Command}
     * @param globalDefaultUsage the global default usage BUILDER.
     */
    void setGlobalDefaultUsage(@NotNull CommandUsage.Builder<S> globalDefaultUsage);


    /**
     * @return the default attachment mode {@link AttachmentMode}
     */
    @NotNull AttachmentMode getDefaultAttachmentMode();

    /**
     * Sets the default {@link AttachmentMode}
     * @param attachmentMode the attachment mode.
     */
    void setDefaultAttachmentMode(AttachmentMode attachmentMode);

    /**
     * @return the help coordinator that coordinates
     * how a help is being shown and displayed to the source.
     */
    @NotNull HelpCoordinator<S> getHelpCoordinator();

    /**
     * Sets the coordinator for displaying of a help.
     * @param coordinator the new coordinator to set.
     */
    void setHelpCoordinator(@NotNull HelpCoordinator<S> coordinator);

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
     * @return the default global command coordinator
     */
    CommandCoordinator<S> getGlobalCommandCoordinator();

    void setGlobalCommandCoordinator(CommandCoordinator<S> commandCoordinator);

    void setCoroutineScope(@NotNull Object scope);

    @Nullable Object getCoroutineScope();

    boolean hasCoroutineScope();

    ImperatConfig<S> setCommandParsingMode(CommandParsingMode mode);

    CommandParsingMode getCommandParsingMode();

    enum CommandParsingMode {
        JAVA,
        KOTLIN,
        AUTO
    }

    EventBus getEventBus();

    void setEventBus(EventBus eventBus);
}
