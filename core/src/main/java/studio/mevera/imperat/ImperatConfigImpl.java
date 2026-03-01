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
import studio.mevera.imperat.command.ContextArgumentProviderRegistry;
import studio.mevera.imperat.command.ReturnResolverRegistry;
import studio.mevera.imperat.command.SourceProviderRegistry;
import studio.mevera.imperat.command.parameters.type.ArgumentType;
import studio.mevera.imperat.command.parameters.type.ArgumentTypeHandler;
import studio.mevera.imperat.command.processors.CommandPostProcessor;
import studio.mevera.imperat.command.processors.CommandPreProcessor;
import studio.mevera.imperat.command.processors.CommandProcessingChain;
import studio.mevera.imperat.command.processors.impl.DefaultProcessors;
import studio.mevera.imperat.command.returns.ReturnResolver;
import studio.mevera.imperat.command.tree.help.HelpCoordinator;
import studio.mevera.imperat.context.ArgumentTypeRegistry;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.ContextFactory;
import studio.mevera.imperat.events.EventBus;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.InvalidSourceException;
import studio.mevera.imperat.exception.ThrowableResolver;
import studio.mevera.imperat.permissions.PermissionChecker;
import studio.mevera.imperat.placeholders.Placeholder;
import studio.mevera.imperat.placeholders.PlaceholderRegistry;
import studio.mevera.imperat.placeholders.PlaceholderResolver;
import studio.mevera.imperat.providers.ContextArgumentProvider;
import studio.mevera.imperat.providers.DependencySupplier;
import studio.mevera.imperat.providers.SourceProvider;
import studio.mevera.imperat.providers.SuggestionProvider;
import studio.mevera.imperat.responses.ResponseKey;
import studio.mevera.imperat.responses.ResponseRegistry;
import studio.mevera.imperat.util.Preconditions;
import studio.mevera.imperat.util.Registry;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

final class ImperatConfigImpl<S extends Source> implements ImperatConfig<S> {

    private EventBus eventBus;
    private CommandParsingMode parsingMode = CommandParsingMode.JAVA;
    private final Registry<Type, DependencySupplier> dependencyResolverRegistry = new Registry<>();
    private final ContextArgumentProviderRegistry<S> contextArgumentProviderRegistry;
    private final ArgumentTypeRegistry<S> argumentTypeRegistry;
    private final SourceProviderRegistry<S> sourceProviderRegistry;
    private final ReturnResolverRegistry<S> returnResolverRegistry;
    private final Map<Class<? extends Throwable>, ThrowableResolver<?, S>> errorHandlers = new HashMap<>();
    private final Map<Class<?>, AnnotationReplacer<?>> annotationReplacerMap = new HashMap<>();
    private InstanceFactory<S> instanceFactory = InstanceFactory.defaultFactory();
    private @NotNull SuggestionProvider<S> defaultSuggestionProvider =
            (context, input) ->
                    Collections.emptyList();
    private @NotNull PermissionChecker<S> permissionChecker = (source, permission) -> true;
    private @NotNull ContextFactory<S> contextFactory;
    private @NotNull CommandProcessingChain<S, CommandPreProcessor<S>> globalPreProcessors;
    private @NotNull CommandProcessingChain<S, CommandPostProcessor<S>> globalPostProcessors;
    private boolean overlapOptionalParameterSuggestions = false;
    private boolean handleExecutionConsecutiveOptionalArgumentsSkip = false;
    private String commandPrefix = "/";
    private CommandPathway.Builder<S> globalDefaultUsage = CommandPathway.<S>builder()
                                                                 .execute((src, ctx) -> {
                                                                     StringBuilder invalidUsage =
                                                                             new StringBuilder("/" + ctx.getRootCommandLabelUsed());
                                                                     var args = ctx.arguments();
                                                                     if (!args.isEmpty()) {
                                                                         invalidUsage.append(" ")
                                                                                 .append(String.join(" ", ctx.arguments()));
                                                                     }
                                                                     var detectedUsage = ctx.getDetectedUsage();
                                                                     throw new CommandException(ResponseKey.INVALID_SYNTAX)
                                                                                   .withContextPlaceholders(ctx)
                                                                                   .withPlaceholder("invalid_usage", invalidUsage.toString())
                                                                                   .withPlaceholder("closest_usage",
                                                                                           "/" + ctx.getRootCommandLabelUsed() + " "
                                                                                                   + (detectedUsage != null
                                                                                                              ? detectedUsage.formatted()
                                                                                                              : ""));
                                                                 });

    private HelpCoordinator<S> helpCoordinator = HelpCoordinator.create();

    private ThrowablePrinter throwablePrinter = ThrowablePrinter.simple();

    private CommandCoordinator<S> commandCoordinator = CommandCoordinator.sync();

    private final ResponseRegistry responseRegistry = ResponseRegistry.createDefault();
    private final PlaceholderRegistry placeholderRegistry = PlaceholderRegistry.createDefault();

    private Object coroutineScope = null; // Will be CoroutineScope if set
    private static final boolean COROUTINES_AVAILABLE;

    static {
        boolean available = false;
        try {
            Class.forName("kotlinx.coroutines.CoroutineScope");
            available = true;
        } catch (ClassNotFoundException e) {
            // Coroutines not available
        }
        COROUTINES_AVAILABLE = available;
    }

    ImperatConfigImpl() {
        contextArgumentProviderRegistry = ContextArgumentProviderRegistry.createDefault();
        argumentTypeRegistry = ArgumentTypeRegistry.createDefault();
        sourceProviderRegistry = SourceProviderRegistry.createDefault();
        returnResolverRegistry = ReturnResolverRegistry.createDefault();
        contextFactory = ContextFactory.defaultFactory();

        globalPreProcessors = CommandProcessingChain.<S>preProcessors()
                                      .then(DefaultProcessors.preUsageCooldown())
                                      .build();

        globalPostProcessors = CommandProcessingChain.<S>postProcessors()
                                       .build();

        // register some defaults:
        this.regDefThrowableResolvers();
        this.registerSourceProvider(Source.class, (source, ctx) -> source);

        this.eventBus = EventBus.createDummy();
    }

    private void regDefThrowableResolvers() {
        this.setThrowableResolver(CommandException.class, (exception, context) -> {
            // CommandException - this is an exception that should be handled and not printed as error
            if (exception.getResponseKey() != null) {
                var response = this.getResponseRegistry().getResponse(exception.getResponseKey());
                if (response != null) {
                    response.sendContent(context, exception.getPlaceholderDataProvider());
                }
            } else {
                context.source().reply(exception.getMessage());
            }
        });
        // InvalidSourceException - this is a system/runtime exception that should still be handled
        this.setThrowableResolver(InvalidSourceException.class, (exception, context) -> {
            throw new UnsupportedOperationException("Couldn't find any source resolver for valueType `"
                                                            + exception.getTargetType().getTypeName() + "'");
        });
    }


    @Override
    public String commandPrefix() {
        return commandPrefix;
    }

    @Override
    public void setCommandPrefix(String cmdPrefix) {
        this.commandPrefix = cmdPrefix;
    }

    /**
     * Sets the whole pre-processing chain
     *
     * @param chain the chain to set
     */
    @Override
    public void setPreProcessorsChain(CommandProcessingChain<S, CommandPreProcessor<S>> chain) {
        Preconditions.notNull(chain, "pre-processors chain");
        this.globalPreProcessors = chain;
    }

    /**
     * Sets the whole post-processing chain
     *
     * @param chain the chain to set
     */
    @Override
    public void setPostProcessorsChain(CommandProcessingChain<S, CommandPostProcessor<S>> chain) {
        Preconditions.notNull(chain, "post-processors chain");
        this.globalPostProcessors = chain;
    }

    /**
     * @return gets the pre-processors in the chain of execution
     * @see CommandPreProcessor
     */
    @Override
    public CommandProcessingChain<S, CommandPreProcessor<S>> getPreProcessors() {
        return globalPreProcessors;
    }

    /**
     * @return gets the post-processors in the chain of execution
     * @see CommandPostProcessor
     */
    @Override
    public CommandProcessingChain<S, CommandPostProcessor<S>> getPostProcessors() {
        return globalPostProcessors;
    }

    /**
     * @return {@link PermissionChecker} for the dispatcher
     */
    @Override
    public @NotNull PermissionChecker<S> getPermissionChecker() {
        return permissionChecker;
    }

    /**
     * Sets the permission resolver for the platform
     *
     * @param permissionChecker the permission resolver to set
     */
    @Override
    public void setPermissionResolver(@NotNull PermissionChecker<S> permissionChecker) {
        this.permissionChecker = permissionChecker;
    }

    /**
     * @return the factory for creation of
     * command related contexts {@link Context}
     */
    @Override
    public @NotNull ContextFactory<S> getContextFactory() {
        return contextFactory;
    }


    /**
     * sets the context factory {@link ContextFactory} for the contexts
     *
     * @param contextFactory the context factory to set
     */
    @Override
    public void setContextFactory(@NotNull ContextFactory<S> contextFactory) {
        this.contextFactory = contextFactory;
    }

    /**
     * Checks whether the valueType has
     * a registered context-resolver
     *
     * @param type the valueType
     * @return whether the valueType has
     * a context-resolver
     */
    @Override
    public boolean hasContextResolver(Type type) {
        return getContextArgumentProvider(type) != null;
    }

    /**
     * @return the registry for responses/messages.
     */
    @Override
    public @NotNull ResponseRegistry getResponseRegistry() {
        return responseRegistry;
    }

    /**
     * Registers a context resolver factory
     *
     * @param factory the factory to register
     */
    @Override
    public <T> void registerContextArgumentProviderFactory(Type type, ContextArgumentProviderFactory<S, T> factory) {
        contextArgumentProviderRegistry.registerFactory(type, factory);
    }

    /**
     * @return returns the factory for creation of
     * {@link ContextArgumentProvider}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> @Nullable ContextArgumentProviderFactory<S, T> getContextResolverFactory(Type type) {
        return (ContextArgumentProviderFactory<S, T>) contextArgumentProviderRegistry.getFactoryFor(type).orElse(null);
    }

    /**
     * Fetches {@link ContextArgumentProvider} for a certain valueType
     *
     * @param resolvingContextType the valueType for this resolver
     * @return the context resolver
     */
    @Override
    public <T> @Nullable ContextArgumentProvider<S, T> getContextArgumentProvider(Type resolvingContextType) {
        return contextArgumentProviderRegistry.getContextArgumentWithoutParameterElement(resolvingContextType);
    }

    /**
     * Fetches the context resolver for {@link ParameterElement} of a method
     *
     * @param element the element
     * @return the {@link ContextArgumentProvider} for this element
     */
    @Override
    public <T> @Nullable ContextArgumentProvider<S, T> getContextArgumentProviderFor(@NotNull ParameterElement element) {
        Preconditions.notNull(element, "element");
        return contextArgumentProviderRegistry.getContextArgumentProvider(element.getType(), element);
    }

    /**
     * Registers {@link ContextArgumentProvider}
     *
     * @param type     the class-valueType of value being resolved from context
     * @param resolver the resolver for this value
     */
    @Override
    public <T> void registerContextArgumentProvider(Type type, @NotNull ContextArgumentProvider<S, T> resolver) {
        contextArgumentProviderRegistry.registerProvider(type, resolver);
    }

    @Override
    public <T> void registerArgType(Type type, @NotNull ArgumentType<S, T> resolver) {
        Preconditions.notNull(type, "type");
        Preconditions.notNull(resolver, "resolver");
        argumentTypeRegistry.registerResolver(type, () -> resolver);
    }


    /**
     * Registers a custom {@link ArgumentTypeHandler}.
     * <p>
     * The handler will be added to the priority list and checked during type resolution
     * based on its priority.
     * </p>
     *
     * @param handler the handler to register
     */
    @Override
    public void registerArgTypeHandler(@NotNull ArgumentTypeHandler<S> handler) {
        argumentTypeRegistry.registerHandler(handler);
    }

    /**
     * Retrieves the {@link ArgumentTypeRegistry} associated with this registrar.
     *
     * @return the {@link ArgumentTypeRegistry} instance
     */
    @Override
    public ArgumentTypeRegistry<S> getArgumentTypeRegistry() {
        return argumentTypeRegistry;
    }

    /**
     * Retrieves the default suggestion resolver associated with this registrar.
     *
     * @return the {@link SuggestionProvider} instance used as the default resolver
     */
    @Override
    public @NotNull SuggestionProvider<S> getDefaultSuggestionResolver() {
        return defaultSuggestionProvider;
    }

    /**
     * Sets the default suggestion resolver to be used when no specific
     * suggestion resolver is provided. The default suggestion resolver
     * handles the auto-completion of arguments/parameters for commands.
     *
     * @param defaultSuggestionProvider the {@link SuggestionProvider} to be set as default
     */
    @Override
    public void setDefaultSuggestionResolver(@NotNull SuggestionProvider<S> defaultSuggestionProvider) {
        this.defaultSuggestionProvider = defaultSuggestionProvider;
    }


    /**
     * Fetches {@link ArgumentType} for a certain value
     *
     * @param resolvingValueType the value that the resolver ends providing it from the context
     * @return the context resolver of a certain valueType
     */
    @Override
    public @Nullable ArgumentType<S, ?> getArgumentType(Type resolvingValueType) {
        return argumentTypeRegistry.getResolver(resolvingValueType).orElse(null);
    }

    @Override
    public <A extends Annotation> void registerAnnotationReplacer(Class<A> type, AnnotationReplacer<A> replacer) {
        annotationReplacerMap.put(type, replacer);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A extends Annotation> void applyAnnotationReplacers(Imperat<S> imperat) {
        this.annotationReplacerMap.forEach((type, replacer) -> {
            Class<A> annType = (Class<A>) type;
            AnnotationReplacer<A> annReplacer = (AnnotationReplacer<A>) replacer;
            imperat.registerAnnotationReplacer(annType, annReplacer);
        });
    }

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
     * {@code false} if only one optional parameter should be suggested at a time
     * @see #setOptionalParameterSuggestionOverlap(boolean)
     */
    @Override
    public boolean isOptionalParameterSuggestionOverlappingEnabled() {
        return overlapOptionalParameterSuggestions;
    }

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
    @Override
    public void setOptionalParameterSuggestionOverlap(boolean enabled) {
        this.overlapOptionalParameterSuggestions = enabled;
    }

    @Override
    public boolean handleExecutionMiddleOptionalSkipping() {
        return handleExecutionConsecutiveOptionalArgumentsSkip;
    }

    @Override
    public void setHandleExecutionConsecutiveOptionalArgumentsSkip(boolean toggle) {
        this.handleExecutionConsecutiveOptionalArgumentsSkip = toggle;
    }


    /**
     * Fetches the suggestion provider/resolver for a specific valueType of
     * argument or parameter.
     *
     * @param type the valueType
     * @return the {@link SuggestionProvider} instance for that valueType
     */
    @Override
    public @Nullable SuggestionProvider<S> getSuggestionProviderForType(Type type) {
        return argumentTypeRegistry.getResolver(type)
                       .map(ArgumentType::getSuggestionProvider)
                       .orElse(null);
    }

    /**
     * Registers a placeholder
     *
     * @param placeholder to register
     */
    @Override
    public void registerPlaceholder(Placeholder placeholder) {
        placeholderRegistry.setData(placeholder.id(), placeholder);
    }

    /**
     * The id/format of this placeholder, must be unique and lowercase
     *
     * @param id the id for the placeholder
     * @return the placeholder
     */
    @Override
    public Optional<Placeholder> getPlaceHolder(String id) {
        return placeholderRegistry.getData(id);
    }

    /**
     * Replaces the placeholders of input by their {@link PlaceholderResolver}
     *
     * @param input the input
     * @return the processed/replaced text input.
     */
    @Override
    public @NotNull String replacePlaceholders(String input) {
        return placeholderRegistry.applyPlaceholders(input);
    }

    /**
     * Replaces the placeholders on each string of the array,
     * modifying the input array content.
     *
     * @param array the array to replace its string contents
     * @return The placeholder replaced String array
     */
    @Override
    public @NotNull String[] replacePlaceholders(String[] array) {
        return placeholderRegistry.resolvedArray(array);
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable <R> SourceProvider<S, R> getSourceProviderFor(Type type) {
        return (SourceProvider<S, R>) sourceProviderRegistry.getData(type).orElse(null);
    }

    @Override
    public <R> void registerSourceProvider(Type type, SourceProvider<S, R> sourceProvider) {
        sourceProviderRegistry.setData(type, sourceProvider);
    }

    @Override
    public @Nullable <T> ReturnResolver<S, T> getReturnResolver(Type type) {
        return returnResolverRegistry.getReturnResolver(type);
    }

    @Override
    public <T> void registerReturnResolver(Type type, ReturnResolver<S, T> returnResolver) {
        returnResolverRegistry.setData(type, returnResolver);
    }

    /**
     * Registers the dependency to the type
     *
     * @param type     the type for the dependency
     * @param resolver the resolver
     */
    @Override
    public void registerDependencyResolver(Type type, DependencySupplier resolver) {
        this.dependencyResolverRegistry.setData(type, resolver);
    }

    /**
     * Resolves dependency of certain type
     *
     * @param type the type
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> @Nullable T resolveDependency(Type type) {
        return (T) dependencyResolverRegistry.getData(type)
                           .map(DependencySupplier::get).orElse(null);
    }

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends Throwable> ThrowableResolver<T, S> getThrowableResolver(Class<T> exception) {
        Class<?> current = exception;
        while (current != null && Throwable.class.isAssignableFrom(current)) {
            var resolver = errorHandlers.get(current);
            if (resolver != null) {
                return (ThrowableResolver<T, S>) resolver;
            }
            current = current.getSuperclass();
        }
        return null;
    }

    @Override
    public <T extends Throwable> void setThrowableResolver(Class<T> exception, ThrowableResolver<T, S> handler) {
        this.errorHandlers.put(exception, handler);
    }

    @Override
    public CommandPathway.@NotNull Builder<S> getGlobalDefaultPathway() {
        return globalDefaultUsage;
    }

    @Override
    public void setGlobalDefaultUsage(CommandPathway.@NotNull Builder<S> globalDefaultUsage) {
        this.globalDefaultUsage = globalDefaultUsage;
    }

    @Override
    public @NotNull HelpCoordinator<S> getHelpCoordinator() {
        return helpCoordinator;
    }

    @Override
    public void setHelpCoordinator(@NotNull HelpCoordinator<S> coordinator) {
        this.helpCoordinator = coordinator;
    }

    @Override
    public InstanceFactory<S> getInstanceFactory() {
        return instanceFactory;
    }

    @Override
    public void setInstanceFactory(InstanceFactory<S> factory) {
        this.instanceFactory = factory;
    }

    @Override
    public CommandCoordinator<S> getGlobalCommandCoordinator() {
        return commandCoordinator;
    }

    @Override
    public void setGlobalCommandCoordinator(CommandCoordinator<S> commandCoordinator) {
        this.commandCoordinator = commandCoordinator;
    }


    @Override
    public @NotNull ThrowablePrinter getThrowablePrinter() {
        return throwablePrinter;
    }

    @Override
    public void setThrowablePrinter(@NotNull ThrowablePrinter printer) {
        this.throwablePrinter = printer;
    }

    @Override
    public <E extends Throwable> boolean handleExecutionThrowable(@NotNull E throwable, Context<S> context, Class<?> owning, String methodName) {

        //First handling the error using the Local(RootCommand's) Error Handler.
        //if its during execution, then let's use the LAST entered RootCommand (root or sub)
        //Since subcommands also can have their own error handlers (aka ThrowableResolver)
        Command<S> cmd = context instanceof ExecutionContext<S> executionContext ? executionContext.getLastUsedCommand() : context.command();
        while (cmd != null) {
            var res = cmd.handleExecutionThrowable(throwable, context, owning, methodName);
            if (res) {
                return true;
            }
            cmd = cmd.getParent();
        }

        //Trying to handle the error from the Central Throwable Handler.
        var res = ImperatConfig.super.handleExecutionThrowable(throwable, context, owning, methodName);
        if (!res) {
            throwablePrinter.print(throwable);
        }
        return true;
    }

    public ImperatConfig<S> setCommandParsingMode(CommandParsingMode mode) {
        this.parsingMode = mode;
        return this;
    }

    public CommandParsingMode getCommandParsingMode() {
        return parsingMode;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void setCoroutineScope(@NotNull Object scope) {
        if (!COROUTINES_AVAILABLE) {
            throw new IllegalStateException(
                "Cannot set coroutine scope - kotlinx.coroutines is not available on the classpath. " +
                    "Add 'org.jetbrains.kotlinx:kotlinx-coroutines-core' dependency to enable coroutine support."
            );
        }

        // Verify it's actually a CoroutineScope using reflection
        try {
            Class<?> scopeClass = Class.forName("kotlinx.coroutines.CoroutineScope");
            if (!scopeClass.isInstance(scope)) {
                throw new IllegalArgumentException(
                    "Provided scope must be an instance of kotlinx.coroutines.CoroutineScope, got: "
                        + scope.getClass().getName()
                );
            }
        } catch (ClassNotFoundException e) {
            // Should never happen since we checked COROUTINES_AVAILABLE
            throw new IllegalStateException("Coroutines check failed", e);
        }

        this.coroutineScope = scope;
    }

    @Override
    public @Nullable Object getCoroutineScope() {
        return coroutineScope;
    }

    @Override
    public boolean hasCoroutineScope() {
        return coroutineScope != null;
    }
}
