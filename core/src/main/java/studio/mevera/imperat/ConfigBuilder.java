package studio.mevera.imperat;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.annotations.base.AnnotationReplacer;
import studio.mevera.imperat.annotations.base.InstanceFactory;
import studio.mevera.imperat.command.AttachmentMode;
import studio.mevera.imperat.command.CommandCoordinator;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.ContextArgumentProviderFactory;
import studio.mevera.imperat.command.parameters.type.ArgumentType;
import studio.mevera.imperat.command.parameters.type.ArgumentTypeHandler;
import studio.mevera.imperat.command.processors.CommandPostProcessor;
import studio.mevera.imperat.command.processors.CommandPreProcessor;
import studio.mevera.imperat.command.processors.CommandProcessingChain;
import studio.mevera.imperat.command.returns.ReturnResolver;
import studio.mevera.imperat.command.tree.help.HelpCoordinator;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.ContextFactory;
import studio.mevera.imperat.events.EventBus;
import studio.mevera.imperat.exception.ThrowableResolver;
import studio.mevera.imperat.permissions.PermissionChecker;
import studio.mevera.imperat.placeholders.Placeholder;
import studio.mevera.imperat.providers.ContextArgumentProvider;
import studio.mevera.imperat.providers.DependencySupplier;
import studio.mevera.imperat.providers.SourceProvider;
import studio.mevera.imperat.providers.SuggestionProvider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A generic abstract builder class for configuring instances of ImperatConfig and creating
 * implementations of the Imperat interface. The builder pattern is utilized to allow
 * fine-grained configuration of various components needed within the command processing system.
 *
 * @param <S> the source type representing the entity or origin of the command (e.g., a user or a system)
 * @param <I> the implementation type that extends Imperat
 */
@SuppressWarnings("unchecked")
public abstract class ConfigBuilder<S extends Source, I extends Imperat<S>, B extends ConfigBuilder<S, I, B>> {

    protected final ImperatConfig<S> config;
    protected ConfigBuilder() {
        config = new ImperatConfigImpl<>();
    }

    /**
     * Sets the event bus to be used for handling events within the configuration.
     *
     * @param bus the {@link EventBus} instance to be set in the configuration
     * @return the current instance of {@code ConfigBuilder} for method chaining
     */
    public B eventBus(EventBus bus) {
        config.setEventBus(bus);
        return (B) this;
    }

    /**
     * Sets the command prefix for the command processing chain.
     *
     * @param cmdPrefix the prefix string to be used before commands
     * @return the updated instance of the ConfigBuilder to allow for method chaining
     */
    public B commandPrefix(String cmdPrefix) {
        config.setCommandPrefix(cmdPrefix);
        return (B) this;
    }

    /**
     * Sets the {@link ThrowablePrinter} used to print unhandled exceptions.
     *
     * @param printer the printer to use
     * @return the current builder instance for chaining
     */
    public B throwablePrinter(ThrowablePrinter printer) {
        config.setThrowablePrinter(printer);
        return (B) this;
    }

    public B globalCoordinator(CommandCoordinator<S> commandCoordinator) {
        config.setGlobalCommandCoordinator(commandCoordinator);
        return (B) this;
    }

    /**
     * Sets a custom {@link PermissionChecker} to determine and resolve permissions
     * for the command sender/source within the platform's configuration.
     *
     * @param permissionChecker the {@link PermissionChecker} implementation used to handle permission checks for commands
     * @return the current {@link ConfigBuilder} instance for method chaining and further configuration
     */
    public B permissionChecker(PermissionChecker<S> permissionChecker) {
        config.setPermissionResolver(permissionChecker);
        return (B) this;
    }


    /**
     * Sets the {@link HelpCoordinator} that coordinates all the core-components of the
     * new help API, to create a coordinator call {@link HelpCoordinator#create()}
     * @param coordinator the coordinator
     * @since 2.0.0
     * @return the current instance of {@code ConfigBuilder} for method chaining
     */
    public B helpCoordinator(HelpCoordinator<S> coordinator) {
        config.setHelpCoordinator(coordinator);
        return (B) this;
    }

    /**
     * Sets the context factory for creating contexts used in command execution.
     *
     * @param contextFactory the context factory to be used for generating contexts
     * @return the current instance of {@code ConfigBuilder} for method chaining
     */
    public B contextFactory(ContextFactory<S> contextFactory) {
        config.setContextFactory(contextFactory);
        return (B) this;
    }

    /**
     * Registers a {@link ReturnResolver}
     * @param type the type of value to return using the return resolver
     * @param returnResolver the return resolving instance.
     * @return the current {@link ConfigBuilder} instance for fluent chaining
     */
    public B returnResolver(Type type, ReturnResolver<S, ?> returnResolver) {
        if (!returnResolver.getType().equals(type)) {
            throw new IllegalArgumentException("The return resolver entered, has a to-return type that does not match the entered type.");
        }
        config.registerReturnResolver(type, returnResolver);
        return (B) this;
    }

    /**
     * Registers a dependency resolver for a specific type and returns the current {@code ConfigBuilder} instance.
     *
     * @param type     the target type for which the dependency resolver is being registered
     * @param resolver the dependency resolver to associate with the specified type
     * @return the current instance of {@code ConfigBuilder} for method chaining
     */
    // Dependency Resolver
    public B dependencyResolver(Type type, DependencySupplier resolver) {
        config.registerDependencyResolver(type, resolver);
        return (B) this;
    }

    /**
     * Registers a custom annotation replacer for the specified annotation type.
     * This allows for dynamic transformation or substitution of annotations during
     * command processing, enabling advanced annotation-based command customization.
     *
     * <p>Annotation replacers are particularly useful for:
     * <ul>
     *   <li>Converting legacy annotation formats to newer ones</li>
     *   <li>Applying conditional annotation logic based on runtime context</li>
     *   <li>Implementing annotation inheritance or composition patterns</li>
     *   <li>Providing backwards compatibility for deprecated annotations</li>
     * </ul>
     *
     * <p><strong>Example usage:</strong>
     * <pre>{@code
     * builder.annotationReplacer(LegacyCommand.class, (annotation, context) -> {
     *     return Command.builder()
     *         .name(annotation.value())
     *         .permission(annotation.permission())
     *         .build();
     * });
     * }</pre>
     *
     * @param <A> the type of annotation to be replaced by the {@link AnnotationReplacer}
     * @param annotationType the class object representing the annotation type to register
     *                      a replacer for, must not be {@code null}
     * @param replacer the annotation replacer implementation that will handle
     *                transformations for the specified annotation type, must not be {@code null}
     * @return this builder instance for method chaining
     *
     * @throws IllegalArgumentException if annotationType or replacer is {@code null}
     * @see AnnotationReplacer
     */
    public <A extends Annotation> B annotationReplacer(Class<A> annotationType, AnnotationReplacer<A> replacer) {
        config.registerAnnotationReplacer(annotationType, replacer);
        return (B) this;
    }

    /**
     * Configures whether multiple optional parameters can be suggested simultaneously
     * during tab completion at the same command depth level. This is a builder method
     * that provides a fluent interface for the underlying configuration setting.
     *
     * <p>This setting affects the behavior of tab completion suggestions without
     * modifying the underlying command structure. The command tree and parameter
     * validation remain unchanged regardless of this setting.
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
     * <p><strong>Default behavior:</strong> The default value depends on the framework
     * configuration, but typically defaults to {@code false} for simpler user experience.
     *
     * @param overlap {@code true} to allow multiple optional parameter suggestions,
     *               {@code false} to limit to one optional parameter suggestion at a time
     * @return this builder instance for method chaining
     *
     * @see ImperatConfig#isOptionalParameterSuggestionOverlappingEnabled()
     * @see SuggestionProvider
     */
    public B overlapOptionalParameterSuggestions(boolean overlap) {
        config.setOptionalParameterSuggestionOverlap(overlap);
        return (B) this;
    }

    /**
     * Sets the default suggestion resolver to be used when no specific resolver
     * is defined for a parameter or command context. This resolver acts as a
     * fallback mechanism for tab completion suggestions.
     *
     * <p>The default suggestion resolver is invoked when:
     * <ul>
     *   <li>A parameter has no specific {@link SuggestionProvider} defined</li>
     *   <li>A command argument requires suggestions but no custom logic exists</li>
     *   <li>Fallback suggestions are needed during error recovery</li>
     * </ul>
     *
     * <p><strong>Example usage:</strong>
     * <pre>{@code
     * builder.setDefaultSuggestionResolver((source, context, input) -> {
     *     // Provide generic suggestions like player names, common values, etc.
     *     return Arrays.asList("player1", "player2", "default_value");
     * });
     * }</pre>
     *
     * <p><strong>Note:</strong> Setting this to {@code null} will disable default
     * suggestions, potentially leaving some parameters without tab completion support.
     *
     * @param resolver the default suggestion resolver implementation, or {@code null}
     *                to disable default suggestions
     * @return this builder instance for method chaining
     *
     * @see SuggestionProvider
     * @see ImperatConfig#getDefaultSuggestionResolver()
     */
    public B setDefaultSuggestionResolver(SuggestionProvider<S> resolver) {
        config.setDefaultSuggestionResolver(resolver);
        return (B) this;
    }

    /**
     * Registers a throwable resolver for a specific exception type.
     * This method allows customizing the handling behavior for specific
     * types of exceptions during application execution.
     *
     * @param <T>       The type of the throwable.
     * @param exception The class object representing the type of the throwable
     *                  for which the resolver will be configured.
     * @param handler   The {@link ThrowableResolver} implementation responsible
     *                  for handling the specified throwable type.
     * @return The current instance of {@code ConfigBuilder}, allowing method
     *         chaining for further configuration.
     */
    // Throwable Resolver
    public <T extends Throwable> B throwableResolver(Class<T> exception, ThrowableResolver<T, S> handler) {
        config.setThrowableResolver(exception, handler);
        return (B) this;
    }

    /**
     * Adds a command pre-processor to the configuration.
     * A command pre-processor is executed before resolving command arguments into values.
     *
     * @param preProcessor the {@link CommandPreProcessor} to be added to the chain of execution
     * @return the current {@link ConfigBuilder} instance for method chaining
     */
    // CommandProcessingChain Pre-Processor
    public B preProcessor(CommandPreProcessor<S> preProcessor) {
        config.getPreProcessors().add(preProcessor);
        return (B) this;
    }

    /**
     * Adds a {@link CommandPostProcessor} to the chain of execution that processes
     * the command context after the resolving of arguments into values.
     *
     * @param postProcessor the post-processor to be added to the execution chain; it processes
     *                      the command context after argument resolution
     * @return the current {@link ConfigBuilder} instance for chaining additional configurations
     */
    // CommandProcessingChain Post-Processor
    public B postProcessor(CommandPostProcessor<S> postProcessor) {
        config.getPostProcessors().add(postProcessor);
        return (B) this;
    }

    /**
     * Configures the pre-processing chain for command processing.
     * This chain defines a series of {@code CommandPreProcessor} instances
     * that will execute before resolving the arguments into values.
     *
     * @param chain the pre-processing chain to set, which consists of
     *              multiple {@code CommandPreProcessor} handlers
     * @return the current {@code ConfigBuilder} instance for method chaining
     */
    public B preProcessingChain(CommandProcessingChain<S, CommandPreProcessor<S>> chain) {
        this.config.setPreProcessorsChain(chain);
        return (B) this;
    }

    /**
     * Sets the post-processing chain for the configuration.
     *
     * @param chain the {@link CommandProcessingChain} of {@link CommandPostProcessor} instances to be set as the post-processing chain
     * @return the {@link ConfigBuilder} instance for method chaining
     */
    public B postProcessingChain(CommandProcessingChain<S, CommandPostProcessor<S>> chain) {
        this.config.setPostProcessorsChain(chain);
        return (B) this;
    }

    /**
     * Registers a context resolver factory for the specified type.
     * This method allows configuring a factory responsible for creating
     * a context resolver for the given type during the command processing.
     *
     * @param <T> the type the resolver factory handles
     * @param type the specific type for which the resolver factory is being set
     * @param factory the context resolver factory to be registered
     * @return this ConfigBuilder instance for method chaining
     */
    // Context Resolver Factory
    public <T> B contextResolverFactory(Type type, ContextArgumentProviderFactory<S, T> factory) {
        config.registerContextResolverFactory(type, factory);
        return (B) this;
    }

    /**
     * Registers a context resolver for a specified type, allowing you to resolve
     * a default value from the context for that type.
     *
     * @param <T>      the type of value being resolved from the context
     * @param type     the class type of the value to be resolved
     * @param resolver the context resolver responsible for providing the default value
     *                 when required
     * @return the updated instance of {@code ConfigBuilder}, enabling fluent configuration
     */
    // Context Resolver
    public <T> B contextResolver(Type type, ContextArgumentProvider<S, T> resolver) {
        config.registerContextResolver(type, resolver);
        return (B) this;
    }

    /**
     * Registers a parameter type and its associated resolver for parsing command arguments.
     *
     * @param <T>      The type of the parameter being registered.
     * @param type     The class representing the type of the parameter being resolved.
     * @param resolver The resolver to handle parsing for the specified parameter type.
     * @return The current instance of {@code ConfigBuilder}, allowing method chaining.
     */
    public <T> B argType(Type type, ArgumentType<S, T> resolver) {
        config.registerArgType(type, resolver);
        return (B) this;
    }

    /**
     * Registers an {@link ArgumentTypeHandler} to the configuration, allowing for custom handling
     * of argument types during command processing. This method enables the addition of custom
     * logic for resolving and managing argument types based on the provided handler implementation.
     *
     * @param handler the {@link ArgumentTypeHandler} instance that defines custom handling logic for argument types
     * @return the current instance of {@code ConfigBuilder} for method chaining and further configuration
     */
    public B argTypeHandler(ArgumentTypeHandler<S> handler) {
        config.registerArgTypeHandler(handler);
        return (B) this;
    }

    /**
     * Applies a consumer function to the current configuration, allowing modifications
     * to be performed directly on the {@code ImperatConfig} instance.
     *
     * @param configConsumer a {@link Consumer} that takes the {@code ImperatConfig<S>} to apply changes.
     *                       The provided consumer may modify the configuration as needed.
     * @return the current {@code B} instance for fluent method chaining.
     */
    public B applyOnConfig(@NotNull Consumer<ImperatConfig<S>> configConsumer) {
        configConsumer.accept(config);
        return (B) this;
    }

    /**
     * Sets the default suggestion resolver for providing autocomplete suggestions
     * for command arguments or parameters in the configuration.
     *
     * @param suggestionProvider the {@link SuggestionProvider} implementation to be
     *                           used as the default resolver for suggestions
     * @return the current {@link ConfigBuilder} instance for method chaining
     */
    public B defaultSuggestionResolver(@NotNull SuggestionProvider<S> suggestionProvider) {
        config.setDefaultSuggestionResolver(suggestionProvider);
        return (B) this;
    }

    /**
     * Registers a {@link SourceProvider} for a specific type to resolve command sources.
     *
     * @param <R>            the resulting type resolved by the source resolver
     * @param type           the type of the source to be resolved
     * @param sourceProvider the source resolver instance that converts the source
     * @return the current {@link ConfigBuilder} instance for method chaining
     */
    // Source Resolver
    public <R> B sourceProvider(Type type, SourceProvider<S, R> sourceProvider) {
        config.registerSourceProvider(type, sourceProvider);
        return (B) this;
    }

    /**
     * Registers a placeholder with the configuration.
     *
     * @param placeholder the placeholder to be registered, containing the unique identifier
     *                    and the dynamic resolver logic that defines how it behaves.
     * @return the current {@link ConfigBuilder} instance for chaining further configuration.
     */
    // Placeholder
    public B placeholder(Placeholder placeholder) {
        config.registerPlaceholder(placeholder);
        return (B) this;
    }

    /**
     * Sets the global default usage builder that will be used for all commands
     * that do not have their own specific usage builder configured.
     *
     * <p>The usage builder is responsible for constructing the usage/syntax data
     * structure that defines how a command should be used, including its arguments,
     * parameters, and expected format. This global default will be applied to all
     * commands registered through this builder unless they explicitly override it
     * with their own usage configuration.
     *
     * <p>This method follows the builder pattern and returns the current builder
     * instance to allow for method chaining.
     *
     * @param usage the {@link studio.mevera.imperat.command.CommandPathway.Builder} to use as the global default
     *              for building command usage/syntax data. Must not be {@code null}.
     * @return this builder instance for method chaining
     * @throws NullPointerException if {@code usage} is {@code null}
     *
     * @see studio.mevera.imperat.command.CommandPathway.Builder
     *
     * @since 1.0.0
     */
    public B globalDefaultUsageBuilder(CommandPathway.Builder<S> usage) {
        config.setGlobalDefaultUsage(usage);
        return (B) this;
    }

    /**
     * Refer to {@link ImperatConfig#setHandleExecutionConsecutiveOptionalArgumentsSkip(boolean)}
     * @param toggle the toggle for this option
     * @return whether this option is enabled or not.
     */
    public B handleMiddleOptionalArgSkipping(boolean toggle) {
        config.setHandleExecutionConsecutiveOptionalArgumentsSkip(toggle);
        return (B) this;
    }

    /**
     * The default attachment mode.
     * @param attachmentMode the default attachment mode
     * @return the default value.
     */
    public B defaultAttachmentMode(AttachmentMode attachmentMode) {
        config.setDefaultAttachmentMode(attachmentMode);
        return (B) this;
    }

    /**
     * Sets the instance factory used for creating instances of classes
     * during command processing and dependency resolution.
     *
     * @param instanceFactory the {@link InstanceFactory} implementation to be used
     *                        for instantiating classes. Must not be {@code null}.
     * @return this builder instance for method chaining
     * @throws NullPointerException if {@code instanceFactory} is {@code null}
     * @see InstanceFactory
     */
    public B instanceFactory(InstanceFactory<S> instanceFactory) {
        config.setInstanceFactory(instanceFactory);
        return (B) this;
    }

    public <T> B visit(Function<ImperatConfig<S>, T> function, Consumer<T> consumer) {
        T result = function.apply(config);
        consumer.accept(result);
        return (B) this;
    }

    /**
     * Builds and returns the final configuration object based on the provided settings and definitions
     * within the builder. This method finalizes the configuration and ensures all dependencies
     * are properly resolved before returning the result.
     *
     * @return the fully constructed and finalized instance of type {@code I}
     */
    public abstract @NotNull I build();

}
