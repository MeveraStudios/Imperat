package studio.mevera.imperat;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.annotations.base.AnnotationReplacer;
import studio.mevera.imperat.annotations.base.InstanceFactory;
import studio.mevera.imperat.annotations.base.element.MethodElement;
import studio.mevera.imperat.annotations.base.element.ParameterElement;
import studio.mevera.imperat.command.CommandCoordinator;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.ContextArgumentProviderFactory;
import studio.mevera.imperat.command.ContextArgumentProviderRegistry;
import studio.mevera.imperat.command.ReturnResolverRegistry;
import studio.mevera.imperat.command.SourceProviderRegistry;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.command.arguments.type.ArgumentTypeHandler;
import studio.mevera.imperat.command.returns.ReturnResolver;
import studio.mevera.imperat.config.BehaviorSettings;
import studio.mevera.imperat.config.CommandErrorDispatcher;
import studio.mevera.imperat.config.CoroutineSupport;
import studio.mevera.imperat.config.registries.AnnotationReplacerRegistry;
import studio.mevera.imperat.config.registries.DependencyRegistry;
import studio.mevera.imperat.config.registries.ErrorHandlerRegistry;
import studio.mevera.imperat.context.ArgumentTypeRegistry;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.internal.ContextFactory;
import studio.mevera.imperat.events.EventBus;
import studio.mevera.imperat.exception.CommandExceptionHandler;
import studio.mevera.imperat.exception.InvalidSyntaxException;
import studio.mevera.imperat.permissions.PermissionChecker;
import studio.mevera.imperat.placeholders.Placeholder;
import studio.mevera.imperat.placeholders.PlaceholderRegistry;
import studio.mevera.imperat.providers.ContextArgumentProvider;
import studio.mevera.imperat.providers.DependencySupplier;
import studio.mevera.imperat.providers.SourceProvider;
import studio.mevera.imperat.providers.SuggestionProvider;
import studio.mevera.imperat.responses.ResponseRegistry;
import studio.mevera.imperat.util.Preconditions;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Optional;

/**
 * Concrete {@link ImperatConfig} backed by per-concern registries plus a
 * {@link BehaviorSettings} bag for scalar flags. The class is intentionally
 * thin: each interface method delegates to either a registry or a strategy
 * field, so there is exactly one place to look when modifying behaviour for
 * a given concern.
 *
 * @param <S> the command-source type
 */
final class ImperatConfigImpl<S extends CommandSource> implements ImperatConfig<S> {

    // --- Registries (one per concern) ---------------------------------
    private final ArgumentTypeRegistry<S> argumentTypeRegistry;
    private final ContextArgumentProviderRegistry<S> contextArgumentProviderRegistry;
    private final SourceProviderRegistry<S> sourceProviderRegistry;
    private final ReturnResolverRegistry<S> returnResolverRegistry;
    private final ResponseRegistry responseRegistry;
    private final PlaceholderRegistry placeholderRegistry;
    private final ErrorHandlerRegistry<S> errorHandlerRegistry;
    private final AnnotationReplacerRegistry annotationReplacerRegistry;
    private final DependencyRegistry dependencyRegistry;

    // --- Settings + dispatcher ----------------------------------------
    private final BehaviorSettings<S> behaviorSettings = new BehaviorSettings<>();
    private final CommandErrorDispatcher<S> errorDispatcher;

    // --- Pluggable strategies (1:1 user-overridable singletons) -------
    private @NotNull PermissionChecker<S> permissionChecker = (source, permission) -> true;
    private @NotNull ContextFactory<S> contextFactory;
    private @NotNull SuggestionProvider<S> defaultSuggestionProvider =
            (context, input) -> Collections.emptyList();
    private InstanceFactory<S> instanceFactory = InstanceFactory.defaultFactory();
    private CommandCoordinator<S> commandCoordinator = CommandCoordinator.sync();
    private ThrowablePrinter throwablePrinter = ThrowablePrinter.simple();
    private EventBus eventBus = EventBus.createDummy();
    private CommandPathway.Builder<S> globalDefaultUsage = CommandPathway.<S>builder()
                                                                 .execute((src, ctx) -> {
                                                                     StringBuilder invalidUsage =
                                                                             new StringBuilder("/" + ctx.getRootCommandLabelUsed());
                                                                     var args = ctx.arguments();
                                                                     if (!args.isEmpty()) {
                                                                         invalidUsage.append(" ")
                                                                                 .append(String.join(" ", ctx.arguments()));
                                                                     }
                                                                     var closestUsage = ctx.command().tree().getClosestPathwayToContext(ctx,
                                                                             ctx.getTreeMatch());

                                                                     throw new InvalidSyntaxException(
                                                                             invalidUsage.toString(),
                                                                             closestUsage
                                                                     );
                                                                 });

    // --- Optional ------------------------------------------------------
    private @Nullable Object coroutineScope;

    ImperatConfigImpl() {
        this.contextArgumentProviderRegistry = ContextArgumentProviderRegistry.createDefault();
        this.argumentTypeRegistry = ArgumentTypeRegistry.createDefault();
        this.sourceProviderRegistry = SourceProviderRegistry.createDefault();
        this.returnResolverRegistry = ReturnResolverRegistry.createDefault();
        this.responseRegistry = ResponseRegistry.createDefault();
        this.placeholderRegistry = PlaceholderRegistry.createDefault();
        // ErrorHandlerRegistry needs ResponseRegistry to be constructed first
        // because the default ResponseException handler reads from it.
        this.errorHandlerRegistry = ErrorHandlerRegistry.createDefault(responseRegistry);
        this.annotationReplacerRegistry = new AnnotationReplacerRegistry();
        this.dependencyRegistry = new DependencyRegistry();
        this.contextFactory = ContextFactory.defaultFactory();
        this.errorDispatcher = new CommandErrorDispatcher<>(errorHandlerRegistry);

        this.registerSourceProvider(CommandSource.class, (source, ctx) -> source);
    }

    // ------------------------------------------------------------------
    // Behaviour scalars (delegated to BehaviorSettings)
    // ------------------------------------------------------------------

    @Override
    public String commandPrefix() {
        return behaviorSettings.commandPrefix();
    }

    @Override
    public void setCommandPrefix(String cmdPrefix) {
        behaviorSettings.setCommandPrefix(cmdPrefix);
    }

    @Override
    public boolean isOptionalParameterSuggestionOverlappingEnabled() {
        return behaviorSettings.isOverlapOptionalParameterSuggestions();
    }

    @Override
    public void setOptionalParameterSuggestionOverlap(boolean enabled) {
        behaviorSettings.setOverlapOptionalParameterSuggestions(enabled);
    }

    @Override
    public boolean handleExecutionMiddleOptionalSkipping() {
        return behaviorSettings.handleExecutionMiddleOptionalSkipping();
    }

    @Override
    public void setHandleExecutionConsecutiveOptionalArgumentsSkip(boolean toggle) {
        behaviorSettings.setHandleExecutionMiddleOptionalSkipping(toggle);
    }

    @Override
    public ImperatConfig<S> setCommandParsingMode(CommandParsingMode mode) {
        behaviorSettings.setParsingMode(mode);
        return this;
    }

    @Override
    public CommandParsingMode getCommandParsingMode() {
        return behaviorSettings.parsingMode();
    }

    // ------------------------------------------------------------------
    // Strategy slots
    // ------------------------------------------------------------------

    @Override
    public @NotNull PermissionChecker<S> getPermissionChecker() {
        return permissionChecker;
    }

    @Override
    public void setPermissionResolver(@NotNull PermissionChecker<S> permissionChecker) {
        this.permissionChecker = permissionChecker;
    }

    @Override
    public @NotNull ContextFactory<S> getContextFactory() {
        return contextFactory;
    }

    @Override
    public void setContextFactory(@NotNull ContextFactory<S> contextFactory) {
        this.contextFactory = contextFactory;
    }

    @Override
    public @NotNull SuggestionProvider<S> getDefaultSuggestionResolver() {
        return defaultSuggestionProvider;
    }

    @Override
    public void setDefaultSuggestionProvider(@NotNull SuggestionProvider<S> defaultSuggestionProvider) {
        this.defaultSuggestionProvider = defaultSuggestionProvider;
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
    public CommandPathway.@NotNull Builder<S> getGlobalDefaultPathway() {
        return globalDefaultUsage;
    }

    @Override
    public void setGlobalDefaultPathway(CommandPathway.@NotNull Builder<S> globalDefaultUsage) {
        this.globalDefaultUsage = globalDefaultUsage;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    // ------------------------------------------------------------------
    // ArgumentType registry
    // ------------------------------------------------------------------

    @Override
    public <T> void registerArgType(Type type, @NotNull ArgumentType<S, T> resolver) {
        Preconditions.notNull(type, "type");
        Preconditions.notNull(resolver, "resolver");
        argumentTypeRegistry.registerResolver(type, () -> resolver);
    }

    @Override
    public void registerArgTypeHandler(@NotNull ArgumentTypeHandler<S> handler) {
        argumentTypeRegistry.registerHandler(handler);
    }

    @Override
    public ArgumentTypeRegistry<S> getArgumentTypeRegistry() {
        return argumentTypeRegistry;
    }

    @Override
    public @Nullable ArgumentType<S, ?> getArgumentType(Type resolvingValueType) {
        return argumentTypeRegistry.getResolver(resolvingValueType).orElse(null);
    }

    @Override
    public @Nullable SuggestionProvider<S> getSuggestionProviderForType(Type type) {
        return argumentTypeRegistry.getResolver(type)
                       .map(ArgumentType::getSuggestionProvider)
                       .orElse(null);
    }

    // ------------------------------------------------------------------
    // ContextArgumentProvider registry
    // ------------------------------------------------------------------

    @Override
    public boolean hasContextResolver(Type type) {
        return getContextArgumentProvider(type) != null;
    }

    @Override
    public <T> void registerContextArgumentProviderFactory(Type type, ContextArgumentProviderFactory<S, T> factory) {
        contextArgumentProviderRegistry.registerFactory(type, factory);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> @Nullable ContextArgumentProviderFactory<S, T> getContextArgumentProviderFactory(Type type) {
        return (ContextArgumentProviderFactory<S, T>) contextArgumentProviderRegistry.getFactoryFor(type).orElse(null);
    }

    @Override
    public <T> @Nullable ContextArgumentProvider<S, T> getContextArgumentProvider(Type resolvingContextType) {
        return contextArgumentProviderRegistry.getContextArgumentWithoutParameterElement(resolvingContextType);
    }

    @Override
    public <T> @Nullable ContextArgumentProvider<S, T> getContextArgumentProviderFor(@NotNull ParameterElement element) {
        Preconditions.notNull(element, "element");
        return contextArgumentProviderRegistry.getContextArgumentProvider(element.getType(), element);
    }

    @Override
    public <T> void registerContextArgumentProvider(Type type, @NotNull ContextArgumentProvider<S, T> resolver) {
        contextArgumentProviderRegistry.registerProvider(type, resolver);
    }

    // ------------------------------------------------------------------
    // SourceProvider / ReturnResolver registries
    // ------------------------------------------------------------------

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
    @SuppressWarnings("unchecked")
    public @Nullable <T> ReturnResolver<S, T> getReturnResolver(MethodElement method) {
        if (method.getSpecificReturnResolver() != null) {
            return (ReturnResolver<S, T>) method.getSpecificReturnResolver();
        }
        return returnResolverRegistry.getReturnResolver(method.getReturnType());
    }

    @Override
    public <T> void registerReturnResolver(Type type, ReturnResolver<S, T> returnResolver) {
        returnResolverRegistry.setData(type, returnResolver);
    }

    // ------------------------------------------------------------------
    // Response + placeholder registries
    // ------------------------------------------------------------------

    @Override
    public @NotNull ResponseRegistry getResponseRegistry() {
        return responseRegistry;
    }

    @Override
    public void registerPlaceholder(Placeholder placeholder) {
        placeholderRegistry.setData(placeholder.id(), placeholder);
    }

    @Override
    public Optional<Placeholder> getPlaceHolder(String id) {
        return placeholderRegistry.getData(id);
    }

    @Override
    public @NotNull String replacePlaceholders(String input) {
        return placeholderRegistry.applyPlaceholders(input);
    }

    @Override
    public @NotNull String[] replacePlaceholders(String[] array) {
        return placeholderRegistry.resolvedArray(array);
    }

    // ------------------------------------------------------------------
    // Dependency registry
    // ------------------------------------------------------------------

    @Override
    public void registerDependencyResolver(Type type, DependencySupplier resolver) {
        dependencyRegistry.register(type, resolver);
    }

    @Override
    public <T> @Nullable T resolveDependency(Type type) {
        return dependencyRegistry.resolve(type);
    }

    // ------------------------------------------------------------------
    // Error-handler registry + dispatcher
    // ------------------------------------------------------------------

    @Override
    public @Nullable <T extends Throwable> CommandExceptionHandler<T, S> getErrorHandlerFor(Class<T> type) {
        return errorHandlerRegistry.getFor(type);
    }

    @Override
    public <T extends Throwable> void setErrorHandler(Class<T> exception, CommandExceptionHandler<T, S> handler) {
        errorHandlerRegistry.register(exception, handler);
    }

    @Override
    public <E extends Throwable> boolean handleExecutionError(@NotNull E throwable, CommandContext<S> context, Class<?> owning,
            String methodName) {
        return errorDispatcher.dispatch(throwable, context, owning, methodName, throwablePrinter);
    }

    // ------------------------------------------------------------------
    // Annotation replacers (staged before the parser exists)
    // ------------------------------------------------------------------

    @Override
    public <A extends Annotation> void registerAnnotationReplacer(Class<A> type, AnnotationReplacer<A> replacer) {
        annotationReplacerRegistry.register(type, replacer);
    }

    /**
     * Package-private bridge: invoked once by {@code BaseImperat}'s constructor
     * to replay every staged {@link AnnotationReplacer} onto the freshly-built
     * annotation parser. Hidden from the public {@link ImperatConfig} surface
     * because users have no reason to call it.
     */
    void installAnnotationReplacersInto(@NotNull AnnotationInjector<S> injector) {
        annotationReplacerRegistry.installInto(injector);
    }

    // ------------------------------------------------------------------
    // Coroutines
    // ------------------------------------------------------------------

    @Override
    public void setCoroutineScope(@NotNull Object scope) {
        this.coroutineScope = CoroutineSupport.requireScope(scope);
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
