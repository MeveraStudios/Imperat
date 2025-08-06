package studio.mevera.imperat;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.annotations.base.AnnotationReplacer;
import studio.mevera.imperat.command.AttachmentMode;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.command.ContextResolverFactory;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.parameters.type.ParameterType;
import studio.mevera.imperat.command.processors.CommandPostProcessor;
import studio.mevera.imperat.command.processors.CommandPreProcessor;
import studio.mevera.imperat.command.processors.CommandProcessingChain;
import studio.mevera.imperat.command.tree.ParameterNode;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.ContextFactory;
import studio.mevera.imperat.exception.ThrowableResolver;
import studio.mevera.imperat.help.HelpProvider;
import studio.mevera.imperat.placeholders.Placeholder;
import studio.mevera.imperat.resolvers.*;
import studio.mevera.imperat.verification.UsageVerifier;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.function.Consumer;

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
     * Sets the command prefix for the command processing chain.
     *
     * @param cmdPrefix the prefix string to be used before commands
     * @return the updated instance of the ConfigBuilder to allow for method chaining
     */
    // CommandProcessingChain Prefix
    public B commandPrefix(String cmdPrefix) {
        config.setCommandPrefix(cmdPrefix);
        return (B) this;
    }

    /**
     * Sets a custom {@link PermissionChecker} to determine and resolve permissions
     * for the command sender/source within the platform's configuration.
     *
     * @param permissionChecker the {@link PermissionChecker} implementation used to handle permission checks for commands
     * @return the current {@link ConfigBuilder} instance for method chaining and further configuration
     */
    // Permission Resolver
    public B permissionChecker(PermissionChecker<S> permissionChecker) {
        config.setPermissionResolver(permissionChecker);
        return (B) this;
    }
    
    /**
     * Toggles the APA (Auto Permission Assign) mode,
     * When enabled, it will automatically compute, set permissions automatically for every {@link CommandParameter}
     * in every {@link CommandUsage} you make without the need to explicitly set the permissions , the same goes
     * on ROOT commands and even subcommands (since they are also treated as {@link CommandParameter})
     *
     * @param modeToggle toggles the auto permission assign mode
     * @return the current {@link ConfigBuilder} instance for method chaining and further configuration
     */
    public B autoPermissionAssignMode(boolean modeToggle) {
        config.setAutoPermissionAssignMode(modeToggle);
        return (B) this;
    }
    
    /**
     * Sets the permission loader for automatic permission assignment.
     * <p>
     * This method configures the {@link PermissionLoader} that will be used to load
     * and resolve permissions for command parameters when Auto Permission Assign (APA)
     * mode is active. The permission loader is responsible for determining what
     * permissions should be applied to individual command nodes based on the
     * command structure and configuration.
     * </p>
     *
     * <p>
     * <strong>Prerequisite:</strong> This method can only be called when APA mode is enabled
     * via the configuration. APA mode must be activated before setting up permission
     * loading components to ensure proper initialization order and prevent
     * configuration conflicts.
     * </p>
     *
     * <p>
     * The permission loader works in conjunction with the {@link NodePermissionAssigner}
     * to provide a complete automatic permission assignment system. The loader determines
     * which permissions to assign, while the assigner handles how those permissions
     * are applied to the parameter nodes.
     * </p>
     *
     * @param permissionLoader the permission loader to use for loading permissions,
     *                        must not be null
     * @return this builder instance for method chaining, never null
     * @throws IllegalStateException if Auto Permission Assign (APA) mode is not enabled
     * @throws NullPointerException if {@code permissionLoader} is null
     * @see PermissionLoader
     * @see NodePermissionAssigner
     * @see #permissionAssigner(NodePermissionAssigner)
     * @since 1.0
     */
    @ApiStatus.Experimental
    public B permissionLoader(PermissionLoader<S> permissionLoader) {
        if(!config.isAutoPermissionAssignMode()) {
            throw new IllegalStateException("Please enable APA(Auto Permission Assign) Mode before doing this");
        }
        config.setPermissionLoader(permissionLoader);
        return (B)this;
    }
    
    /**
     * Sets the permission assigner for automatic permission assignment.
     * <p>
     * This method configures the {@link NodePermissionAssigner} that will be used to
     * assign permissions to command parameter nodes when Auto Permission Assign (APA)
     * mode is active. The permission assigner defines the strategy for how permissions
     * loaded by the {@link PermissionLoader} are actually applied to individual
     * {@link ParameterNode} instances.
     * </p>
     *
     * <p>
     * <strong>Prerequisite:</strong> This method can only be called when APA mode is enabled
     * via the configuration. APA mode must be activated before setting up permission
     * assignment components to ensure proper initialization order and prevent
     * configuration conflicts.
     * </p>
     *
     * <p>
     * The permission assigner works in conjunction with the {@link PermissionLoader}
     * to provide a complete automatic permission assignment system. The loader determines
     * which permissions to assign, while the assigner handles how those permissions
     * are applied to the parameter nodes.
     * </p>
     *
     * <p>
     * If no custom assigner is provided, the system will use the default assigner
     * available via {@link NodePermissionAssigner#defaultAssigner()}.
     * </p>
     *
     * @param permissionAssigner the permission assigner to use for assigning permissions
     *                          to parameter nodes, must not be null
     * @return this builder instance for method chaining, never null
     * @throws IllegalStateException if Auto Permission Assign (APA) mode is not enabled
     * @throws NullPointerException if {@code permissionAssigner} is null
     * @see NodePermissionAssigner
     * @see PermissionLoader
     * @see #permissionLoader(PermissionLoader)
     * @see NodePermissionAssigner#defaultAssigner()
     * @since 1.0
     */
    @ApiStatus.Experimental
    public B permissionAssigner(NodePermissionAssigner<S> permissionAssigner) {
        if(!config.isAutoPermissionAssignMode()) {
            throw new IllegalStateException("Please enable APA(Auto Permission Assign) Mode before doing this");
        }
        config.setNodePermissionAssigner(permissionAssigner);
        return (B)this;
    }
    

    /**
     * Sets the context factory for creating contexts used in command execution.
     *
     * @param contextFactory the context factory to be used for generating contexts
     * @return the current instance of {@code ConfigBuilder} for method chaining
     */
    // Context Factory
    public B contextFactory(ContextFactory<S> contextFactory) {
        config.setContextFactory(contextFactory);
        return (B) this;
    }

    /**
     * Sets the usage verifier for the configuration.
     *
     * @param usageVerifier the {@link UsageVerifier} instance to validate command usages
     * @return the current {@link ConfigBuilder} instance for fluent chaining
     */
    // Usage Verifier
    public B usageVerifier(UsageVerifier<S> usageVerifier) {
        config.setUsageVerifier(usageVerifier);
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
    
    public <A extends Annotation> B annotationReplacer(Class<A> annotationType, AnnotationReplacer<A> replacer) {
        config.registerAnnotationReplacer(annotationType, replacer);
        return (B) this;
    }
    
    public B overlapOptionalParameterSuggestions(boolean overlap) {
        config.setOptionalParameterSuggestionOverlap(overlap);
        return (B) this;
    }
    
    public B setDefaultSuggestionResolver(SuggestionResolver<S> resolver) {
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
    public <T extends Throwable> B throwableResolver(
        Class<T> exception, ThrowableResolver<T, S> handler) {
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
    public <T> B contextResolverFactory(Type type, ContextResolverFactory<S, T> factory) {
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
    public <T> B contextResolver(Type type, ContextResolver<S, T> resolver) {
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
    // Parameter Type
    public <T> B parameterType(Type type, ParameterType<S, T> resolver) {
        config.registerParamType(type, resolver);
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
     * Registers a named suggestion resolver for providing autocomplete suggestions
     * for command arguments or parameters.
     *
     * @param name               the unique name to identify the suggestion resolver
     * @param suggestionResolver the suggestion resolver to be registered
     * @return the current instance of {@code ConfigBuilder} for method chaining
     */
    // Named Suggestion Resolver
    public B namedSuggestionResolver(String name, SuggestionResolver<S> suggestionResolver) {
        config.registerNamedSuggestionResolver(name, suggestionResolver);
        return (B) this;
    }

    /**
     * Sets the default suggestion resolver for providing autocomplete suggestions
     * for command arguments or parameters in the configuration.
     *
     * @param suggestionResolver the {@link SuggestionResolver} implementation to be
     *                           used as the default resolver for suggestions
     * @return the current {@link ConfigBuilder} instance for method chaining
     */
    public B defaultSuggestionResolver(@NotNull SuggestionResolver<S> suggestionResolver) {
        config.setDefaultSuggestionResolver(suggestionResolver);
        return (B) this;
    }

    /**
     * Registers a {@link SourceResolver} for a specific type to resolve command sources.
     *
     * @param <R>            the resulting type resolved by the source resolver
     * @param type           the type of the source to be resolved
     * @param sourceResolver the source resolver instance that converts the source
     * @return the current {@link ConfigBuilder} instance for method chaining
     */
    // Source Resolver
    public <R> B sourceResolver(Type type, SourceResolver<S, R> sourceResolver) {
        config.registerSourceResolver(type, sourceResolver);
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
    public B placeholder(Placeholder<S> placeholder) {
        config.registerPlaceholder(placeholder);
        return (B) this;
    }

    /**
     * Sets the help provider to be used for providing help messages.
     *
     * @param helpProvider the help provider instance used to display help messages
     * @return the current instance of {@code ConfigBuilder}, for method chaining
     */
    // Help Provider
    public B helpProvider(HelpProvider<S> helpProvider) {
        config.setHelpProvider(helpProvider);
        return (B) this;
    }


    /**
     * Sets the command tree to be strict
     * @param strict whether the command tree will be strict or not
     * @return the current instance of {@link ConfigBuilder}, for method chaining.
     */
    public B strictCommandTree(boolean strict) {
        this.config.setStrictCommandTree(strict);
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
     * @param usage the {@link CommandUsage.Builder} to use as the global default
     *              for building command usage/syntax data. Must not be {@code null}.
     * @return this builder instance for method chaining
     * @throws NullPointerException if {@code usage} is {@code null}
     *
     * @see CommandUsage.Builder
     *
     * @since 1.0.0
     */
    public B globalDefaultUsageBuilder(CommandUsage.Builder<S> usage) {
        config.setGlobalDefaultUsage(usage);
        return (B) this;
    }
    
    /**
     * Refer to {@link ImperatConfig#setHandleExecutionConsecutiveOptionalArgumentsSkip(boolean)}
     * @param toggle the toggle for this option
     * @return whether this option is enabled or not.
     */
    public B handleExecutionConsecutiveOptionalArguments(boolean toggle) {
        config.setHandleExecutionConsecutiveOptionalArgumentsSkip(toggle);
        return (B)this;
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
     * Builds and returns the final configuration object based on the provided settings and definitions
     * within the builder. This method finalizes the configuration and ensures all dependencies
     * are properly resolved before returning the result.
     *
     * @return the fully constructed and finalized instance of type {@code I}
     */
    public abstract @NotNull I build();

}
