package studio.mevera.imperat;

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
import studio.mevera.imperat.command.ContextResolverRegistry;
import studio.mevera.imperat.command.ReturnResolverRegistry;
import studio.mevera.imperat.command.SourceResolverRegistry;
import studio.mevera.imperat.command.parameters.NumericRange;
import studio.mevera.imperat.command.parameters.type.ParameterType;
import studio.mevera.imperat.command.processors.CommandPostProcessor;
import studio.mevera.imperat.command.processors.CommandPreProcessor;
import studio.mevera.imperat.command.processors.CommandProcessingChain;
import studio.mevera.imperat.command.processors.impl.DefaultProcessors;
import studio.mevera.imperat.command.returns.ReturnResolver;
import studio.mevera.imperat.command.suggestions.SuggestionResolverRegistry;
import studio.mevera.imperat.command.tree.help.HelpCoordinator;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.ParamTypeRegistry;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.ContextFactory;
import studio.mevera.imperat.exception.CooldownException;
import studio.mevera.imperat.exception.FlagOutsideCommandScopeException;
import studio.mevera.imperat.exception.InvalidSourceException;
import studio.mevera.imperat.exception.InvalidSyntaxException;
import studio.mevera.imperat.exception.InvalidUUIDException;
import studio.mevera.imperat.exception.MissingFlagInputException;
import studio.mevera.imperat.exception.NoHelpException;
import studio.mevera.imperat.exception.NoHelpPageException;
import studio.mevera.imperat.exception.NumberOutOfRangeException;
import studio.mevera.imperat.exception.PermissionDeniedException;
import studio.mevera.imperat.exception.SourceException;
import studio.mevera.imperat.exception.ThrowableResolver;
import studio.mevera.imperat.exception.UnknownCommandException;
import studio.mevera.imperat.exception.UnknownFlagException;
import studio.mevera.imperat.exception.parse.InvalidBooleanException;
import studio.mevera.imperat.exception.parse.InvalidEnumException;
import studio.mevera.imperat.exception.parse.InvalidMapEntryFormatException;
import studio.mevera.imperat.exception.parse.InvalidNumberFormatException;
import studio.mevera.imperat.exception.parse.UnknownSubCommandException;
import studio.mevera.imperat.exception.parse.ValueOutOfConstraintException;
import studio.mevera.imperat.exception.parse.WordOutOfRestrictionsException;
import studio.mevera.imperat.permissions.PermissionChecker;
import studio.mevera.imperat.placeholders.Placeholder;
import studio.mevera.imperat.placeholders.PlaceholderRegistry;
import studio.mevera.imperat.placeholders.PlaceholderResolver;
import studio.mevera.imperat.resolvers.ContextResolver;
import studio.mevera.imperat.resolvers.DependencySupplier;
import studio.mevera.imperat.resolvers.SourceResolver;
import studio.mevera.imperat.resolvers.SuggestionResolver;
import studio.mevera.imperat.util.Preconditions;
import studio.mevera.imperat.util.Registry;
import studio.mevera.imperat.util.TypeWrap;
import studio.mevera.imperat.verification.UsageVerifier;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

final class ImperatConfigImpl<S extends Source> implements ImperatConfig<S> {

    private final Registry<Type, DependencySupplier> dependencyResolverRegistry = new Registry<>();
    private final ContextResolverRegistry<S> contextResolverRegistry;
    private final ParamTypeRegistry<S> paramTypeRegistry;
    private final SuggestionResolverRegistry<S> suggestionResolverRegistry;
    private final PlaceholderRegistry<S> placeholderRegistry;
    private final SourceResolverRegistry<S> sourceResolverRegistry;
    private final ReturnResolverRegistry<S> returnResolverRegistry;
    private final Map<Class<? extends Throwable>, ThrowableResolver<?, S>> handlers = new HashMap<>();
    private final Map<Class<?>, AnnotationReplacer<?>> annotationReplacerMap = new HashMap<>();
    private InstanceFactory<S> instanceFactory = InstanceFactory.defaultFactory();
    private @NotNull SuggestionResolver<S> defaultSuggestionResolver =
            (context, input) ->
                    Collections.emptyList();
    private @NotNull PermissionChecker<S> permissionChecker = (source, permission) -> true;
    private @NotNull ContextFactory<S> contextFactory;
    private @NotNull UsageVerifier<S> verifier;
    private @NotNull CommandProcessingChain<S, CommandPreProcessor<S>> globalPreProcessors;
    private @NotNull CommandProcessingChain<S, CommandPostProcessor<S>> globalPostProcessors;
    private boolean overlapOptionalParameterSuggestions = false;
    private boolean handleExecutionConsecutiveOptionalArgumentsSkip = false;
    private String commandPrefix = "/";
    private CommandUsage.Builder<S> globalDefaultUsage = CommandUsage.builder();

    private AttachmentMode defaultAttachmentMode = AttachmentMode.UNSET;

    private HelpCoordinator<S> helpCoordinator = HelpCoordinator.create();

    private ThrowablePrinter throwablePrinter = ThrowablePrinter.simple();

    private CommandCoordinator<S> commandCoordinator = CommandCoordinator.sync();

    ImperatConfigImpl() {
        contextResolverRegistry = ContextResolverRegistry.createDefault();
        paramTypeRegistry = ParamTypeRegistry.createDefault();
        suggestionResolverRegistry = SuggestionResolverRegistry.createDefault(this);
        sourceResolverRegistry = SourceResolverRegistry.createDefault();
        returnResolverRegistry = ReturnResolverRegistry.createDefault();
        placeholderRegistry = PlaceholderRegistry.createDefault(this);
        contextFactory = ContextFactory.defaultFactory();

        verifier = UsageVerifier.typeTolerantVerifier();

        globalPreProcessors = CommandProcessingChain.<S>preProcessors()
                                      .then(DefaultProcessors.preUsageCooldown())
                                      .build();

        globalPostProcessors = CommandProcessingChain.<S>postProcessors()
                                       .build();

        // register some defaults:
        this.regDefThrowableResolvers();
        this.registerSourceResolver(Source.class, (source, ctx) -> source);
    }

    private void regDefThrowableResolvers() {

        // This throwable resolver is intended for CLI applications that lack a low-level command dispatcher
        // to validate whether a command exists. It is needed for non-minecraft java applications.
        this.setThrowableResolver(UnknownCommandException.class, (exception, context) -> {
            context.source().error("No command named '" + exception.getCommand() + "' is registered");
        });

        this.setThrowableResolver(InvalidSourceException.class, (exception, context) -> {
            throw new UnsupportedOperationException("Couldn't find any source resolver for valueType `"
                                                            + exception.getTargetType().getTypeName() + "'");
        });

        this.setThrowableResolver(UnknownFlagException.class, (ex, context) -> {
            context.source().error("Unknown flag '" + ex.getInput() + "'");
        });

        this.setThrowableResolver(MissingFlagInputException.class, (ex, context) -> {
            context.source().error("Please enter the value for flag(s) '" + String.join(",", ex.getFlagData()) + "'");
        });

        this.setThrowableResolver(FlagOutsideCommandScopeException.class, (ex, context) -> {

            context.source().error("Flag(s) '" + ex.getFlagInput() + "' were used (in " + ex.getWrongCmd().name() + "'s scope) outside of their "
                                           + "command's scope");
        });

        this.setThrowableResolver(ValueOutOfConstraintException.class, (ex, context) -> {
            context.source().error("Input '" + ex.getInput() + "' is not one of: [" + String.join(",", ex.getAllowedValues()) + "]");
        });

        this.setThrowableResolver(WordOutOfRestrictionsException.class, (ex, context) -> {
            context.source().error("Word '" + ex.getInput() + "' is not within the given restrictions=" + String.join(",", ex.getRestrictions()));
        });

        this.setThrowableResolver(UnknownSubCommandException.class, (exception, context) -> {
            context.source().error("Unknown sub-command '" + exception.getInput() + "'");
        });

        this.setThrowableResolver(InvalidMapEntryFormatException.class, (exception, context) -> {
            InvalidMapEntryFormatException.Reason reason = exception.getReason();
            String extraMsg = "";
            if (reason == InvalidMapEntryFormatException.Reason.MISSING_SEPARATOR) {
                extraMsg = "entry doesn't contain '" + exception.getRequiredSeparator() + "'";
            } else if (reason == InvalidMapEntryFormatException.Reason.NOT_TWO_ELEMENTS) {
                extraMsg = "entry is not made of 2 elements";
            }
            context.source().error("Invalid map entry '" + exception.getInput() + "'" + (!extraMsg.isEmpty() ? ", " + extraMsg : ""));
        });

        this.setThrowableResolver(InvalidBooleanException.class, (exception, context) -> {
            context.source().error("Invalid boolean '" + exception.getInput() + "'");
        });

        this.setThrowableResolver(InvalidEnumException.class, (exception, context) -> {
            context.source().error("Invalid " + exception.getEnumType().getTypeName() + " '" + exception.getInput() + "'");
        });

        this.setThrowableResolver(InvalidNumberFormatException.class, (exception, context) -> {
            context.source().error("Invalid " + exception.getNumberTypeDisplay() + " format '" + exception.getInput() + "'");
        });

        this.setThrowableResolver(NumberOutOfRangeException.class, ((exception, context) -> {
            NumericRange range = exception.getRange();
            final StringBuilder builder = new StringBuilder();
            if (range.getMin() != Double.MIN_VALUE && range.getMax() != Double.MAX_VALUE) {
                builder.append("within ").append(range.getMin()).append('-').append(range.getMax());
            } else if (range.getMin() != Double.MIN_VALUE) {
                builder.append("at least '").append(range.getMin()).append("'");
            } else if (range.getMax() != Double.MAX_VALUE) {
                builder.append("at most '").append(range.getMax()).append("'");
            } else {
                builder.append("(Open range)");
            }

            String rangeFormatted = builder.toString();
            context.source().error("Value '" + exception.getValue() + "' entered for parameter '" + exception.getParameter().format() + "' must be "
                                           + rangeFormatted);
        }));

        this.setThrowableResolver(
                SourceException.class,
                (exception, context) -> {
                    final String msg = exception.getMessage();
                    switch (exception.getType()) {
                        case SEVERE -> context.source().error(msg);
                        case WARN -> context.source().warn(msg);
                        case REPLY -> context.source().reply(msg);
                    }
                }
        );

        this.setThrowableResolver(
                InvalidUUIDException.class, (exception, context) ->
                                                    context.source().error("Invalid uuid-format '" + exception.getInput() + "'")
        );

        this.setThrowableResolver(
                CooldownException.class,
                (exception, context) -> {
                    context.source().error(
                            "Please wait %d second(s) to execute this command again!".formatted(exception.getRemainingDuration().toSeconds())
                    );
                }
        );
        this.setThrowableResolver(
                PermissionDeniedException.class,
                (exception, context) -> context.source().error("You don't have permission to use this command!")
        );
        this.setThrowableResolver(
                InvalidSyntaxException.class,
                (exception, context) -> {
                    S source = context.source();
                    //if usage is null, find the closest usage
                    source.error("Invalid command usage '/" + context.command().name() + " " + context.arguments().join(" ") + "'");

                    var closestUsage = exception.getExecutionResult().getClosestUsage();
                    if (closestUsage != null) {
                        source.error("Closest Command Usage: " + (context.imperatConfig().commandPrefix() + CommandUsage.format(context.label(),
                                closestUsage)));
                    }
                }
        );

        this.setThrowableResolver(
                NoHelpException.class,
                (exception, context) -> {
                    Command<S> cmdUsed;
                    if (context instanceof ExecutionContext<S> resolvedContext) {
                        cmdUsed = resolvedContext.getLastUsedCommand();
                    } else {
                        cmdUsed = context.command();
                    }
                    assert cmdUsed != null;
                    context.source().error("No Help available for '<command>'".replace("<command>", cmdUsed.name()));
                }
        );
        this.setThrowableResolver(
                NoHelpPageException.class,
                (exception, context) -> {
                    if (!(context instanceof ExecutionContext<S> resolvedContext) || resolvedContext.getDetectedUsage() == null) {
                        throw new IllegalCallerException("Called NoHelpPageCaption in wrong the wrong sequence/part of the code");
                    }

                    int page = resolvedContext.getArgumentOr("page", 1);
                    context.source().error("Page '<page>' doesn't exist!".replace("<page>", String.valueOf(page)));
                }
        );
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
        return getContextResolver(type) != null;
    }

    /**
     * Registers a context resolver factory
     *
     * @param factory the factory to register
     */
    @Override
    public <T> void registerContextResolverFactory(Type type, ContextResolverFactory<S, T> factory) {
        contextResolverRegistry.registerFactory(type, factory);
    }

    /**
     * @return returns the factory for creation of
     * {@link ContextResolver}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> @Nullable ContextResolverFactory<S, T> getContextResolverFactory(Type type) {
        return (ContextResolverFactory<S, T>) contextResolverRegistry.getFactoryFor(type).orElse(null);
    }

    /**
     * Fetches {@link ContextResolver} for a certain valueType
     *
     * @param resolvingContextType the valueType for this resolver
     * @return the context resolver
     */
    @Override
    public <T> @Nullable ContextResolver<S, T> getContextResolver(Type resolvingContextType) {
        return contextResolverRegistry.getResolverWithoutParameterElement(resolvingContextType);
    }

    /**
     * Fetches the context resolver for {@link ParameterElement} of a method
     *
     * @param element the element
     * @return the {@link ContextResolver} for this element
     */
    @Override
    public <T> @Nullable ContextResolver<S, T> getMethodParamContextResolver(@NotNull ParameterElement element) {
        Preconditions.notNull(element, "element");
        return contextResolverRegistry.getContextResolver(element.getType(), element);
    }

    /**
     * Registers {@link ContextResolver}
     *
     * @param type     the class-valueType of value being resolved from context
     * @param resolver the resolver for this value
     */
    @Override
    public <T> void registerContextResolver(Type type, @NotNull ContextResolver<S, T> resolver) {
        contextResolverRegistry.registerResolver(type, resolver);
    }

    /**
     * Registers {@link ParameterType}
     *
     * @param type     the class-valueType of value being resolved from context
     * @param resolver the resolver for this value
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> void registerParamType(Type type, @NotNull ParameterType<S, T> resolver) {
        Preconditions.notNull(type, "type");
        Preconditions.notNull(resolver, "resolver");
        paramTypeRegistry.registerResolver(type, () -> resolver);

        Class<T> rawType = (Class<T>) TypeWrap.of(type).getRawType();
        paramTypeRegistry.registerArrayInitializer(rawType, (length) -> (Object[]) Array.newInstance(rawType, length));
    }

    /**
     * Registers a supplier function that provides new instances of a specific Collection type.
     * This allows the framework to create appropriate collection instances during deserialization
     * or initialization processes.
     *
     * @param collectionType      the Class object representing the collection type
     * @param newInstanceSupplier a Supplier that creates new instances of the collection type
     * @throws NullPointerException     if collectionType or newInstanceSupplier is null
     */
    @Override
    public <C extends Collection<?>> void registerCollectionInitializer(Class<C> collectionType, Supplier<C> newInstanceSupplier) {
        Preconditions.notNull(collectionType, "collectionType");
        Preconditions.notNull(newInstanceSupplier, "newInstanceSupplier");
        paramTypeRegistry.registerCollectionInitializer(collectionType, newInstanceSupplier);
    }

    /**
     * Registers a supplier function that provides new instances of a specific Map type.
     * This allows the framework to create appropriate map instances during deserialization
     * or initialization processes.
     *
     * @param mapType             the Class object representing the map type
     * @param newInstanceSupplier a Supplier that creates new instances of the map type
     * @throws NullPointerException     if mapType or newInstanceSupplier is null
     */
    @Override
    public <M extends Map<?, ?>> void registerMapInitializer(Class<M> mapType, Supplier<M> newInstanceSupplier) {
        Preconditions.notNull(mapType, "mapType");
        Preconditions.notNull(newInstanceSupplier, "newInstanceSupplier");
        paramTypeRegistry.registerMapInitializer(mapType, newInstanceSupplier);
    }

    /**
     * Retrieves the default suggestion resolver associated with this registrar.
     *
     * @return the {@link SuggestionResolver} instance used as the default resolver
     */
    @Override
    public @NotNull SuggestionResolver<S> getDefaultSuggestionResolver() {
        return defaultSuggestionResolver;
    }

    /**
     * Sets the default suggestion resolver to be used when no specific
     * suggestion resolver is provided. The default suggestion resolver
     * handles the auto-completion of arguments/parameters for commands.
     *
     * @param defaultSuggestionResolver the {@link SuggestionResolver} to be set as default
     */
    @Override
    public void setDefaultSuggestionResolver(@NotNull SuggestionResolver<S> defaultSuggestionResolver) {
        this.defaultSuggestionResolver = defaultSuggestionResolver;
    }


    /**
     * Fetches {@link ParameterType} for a certain value
     *
     * @param resolvingValueType the value that the resolver ends providing it from the context
     * @return the context resolver of a certain valueType
     */
    @Override
    public @Nullable ParameterType<S, ?> getParameterType(Type resolvingValueType) {
        return paramTypeRegistry.getResolver(resolvingValueType).orElse(null);
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
     *   <li>Command structure - the actual command tree remains unchanged</li>
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
     * @return the {@link SuggestionResolver} instance for that valueType
     */
    @Override
    public @Nullable SuggestionResolver<S> getSuggestionResolverByType(Type type) {
        return paramTypeRegistry.getResolver(type)
                       .map(ParameterType::getSuggestionResolver)
                       .orElse(null);
    }


    /**
     * Fetches the suggestion provider/resolver for a specific argument
     *
     * @param name the name of the argument
     * @return the {@link SuggestionResolver} instance for that argument
     */
    public @Nullable SuggestionResolver<S> getNamedSuggestionResolver(String name) {
        return suggestionResolverRegistry.getResolverByName(name);
    }

    /**
     * Registers a suggestion resolver linked
     * directly to a unique name
     *
     * @param name               the unique name/id of the suggestion resolver
     * @param suggestionResolver the suggestion resolver to register
     */
    @Override
    public void registerNamedSuggestionResolver(String name, SuggestionResolver<S> suggestionResolver) {
        suggestionResolverRegistry.registerNamedResolver(name.toLowerCase(), suggestionResolver);
    }

    /**
     * Registers a placeholder
     *
     * @param placeholder to register
     */
    @Override
    public void registerPlaceholder(Placeholder<S> placeholder) {
        placeholderRegistry.setData(placeholder.id(), placeholder);
    }

    /**
     * The id/format of this placeholder, must be unique and lowercase
     *
     * @param id the id for the placeholder
     * @return the placeholder
     */
    @Override
    public Optional<Placeholder<S>> getPlaceHolder(String id) {
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
        return placeholderRegistry.resolvedString(input);
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
    public @Nullable <R> SourceResolver<S, R> getSourceResolver(Type type) {
        return (SourceResolver<S, R>) sourceResolverRegistry.getData(type).orElse(null);
    }

    @Override
    public <R> void registerSourceResolver(Type type, SourceResolver<S, R> sourceResolver) {
        sourceResolverRegistry.setData(type, sourceResolver);
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

    /**
     * @return the usage verifier
     */
    @Override
    public UsageVerifier<S> getUsageVerifier() {
        return verifier;
    }

    /**
     * Sets the usage verifier to a new instance
     *
     * @param usageVerifier the usage verifier to set
     */
    @Override
    public void setUsageVerifier(UsageVerifier<S> usageVerifier) {
        this.verifier = usageVerifier;
    }

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends Throwable> ThrowableResolver<T, S> getThrowableResolver(Class<T> exception) {
        Class<?> current = exception;
        while (current != null && Throwable.class.isAssignableFrom(current)) {
            var resolver = handlers.get(current);
            if (resolver != null) {
                return (ThrowableResolver<T, S>) resolver;
            }
            current = current.getSuperclass();
        }
        return null;
    }

    @Override
    public <T extends Throwable> void setThrowableResolver(Class<T> exception, ThrowableResolver<T, S> handler) {
        this.handlers.put(exception, handler);
    }

    @Override
    public CommandUsage.@NotNull Builder<S> getGlobalDefaultUsage() {
        return globalDefaultUsage;
    }

    @Override
    public void setGlobalDefaultUsage(CommandUsage.@NotNull Builder<S> globalDefaultUsage) {
        this.globalDefaultUsage = globalDefaultUsage;
    }

    @Override
    public @NotNull AttachmentMode getDefaultAttachmentMode() {
        return defaultAttachmentMode;
    }

    @Override
    public void setDefaultAttachmentMode(AttachmentMode attachmentMode) {
        this.defaultAttachmentMode = attachmentMode;
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

        //First handling the error using the Local(Command's) Error Handler.
        //if its during execution, then let's use the LAST entered Command (root or sub)
        //Since subcommands also can have their own error handlers (aka ThrowableResolver)
        Command<S> cmd = context instanceof ExecutionContext<S> executionContext ? executionContext.getLastUsedCommand() : context.command();
        while (cmd != null) {
            var res = cmd.handleExecutionThrowable(throwable, context, owning, methodName);
            if (res) {
                return true;
            }
            cmd = cmd.parent();
        }

        //Trying to handle the error from the Central Throwable Handler.
        var res = ImperatConfig.super.handleExecutionThrowable(throwable, context, owning, methodName);
        if (!res) {
            throwablePrinter.print(throwable);
        }
        return true;
    }
}
