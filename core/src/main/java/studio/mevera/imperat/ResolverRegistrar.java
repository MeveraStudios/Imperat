package studio.mevera.imperat;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.ContextResolverFactory;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.command.parameters.type.ArgumentType;
import studio.mevera.imperat.command.parameters.type.ArgumentTypeHandler;
import studio.mevera.imperat.command.returns.ReturnResolver;
import studio.mevera.imperat.context.ArgumentTypeRegistry;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.placeholders.Placeholder;
import studio.mevera.imperat.resolvers.ContextResolver;
import studio.mevera.imperat.resolvers.SourceResolver;
import studio.mevera.imperat.resolvers.SuggestionResolver;
import studio.mevera.imperat.util.TypeWrap;

import java.lang.reflect.Type;
import java.util.Objects;

/**
 * The ResolverRegistrar interface provides mechanisms for registering and retrieving various
 * types of resolvers, factories, and initializers that are contextually linked to specific
 * source types and value types. It is a sealed interface, which limits its implementation
 * to specific permitted types.
 *
 * @param <S> the type of source that this registrar handles
 */
public sealed interface ResolverRegistrar<S extends Source> permits ImperatConfig {


    /**
     * Registers a context resolver factory
     *
     * @param factory the factory to register
     */
    <T> void registerContextResolverFactory(Type type, ContextResolverFactory<S, T> factory);


    /**
     * Registers {@link ContextResolver}
     *
     * @param type     the class-valueType of value being resolved from context
     * @param resolver the resolver for this value
     * @param <T>      the valueType of value being resolved from context
     */
    <T> void registerContextResolver(Type type, @NotNull ContextResolver<S, T> resolver);


    /**
     * Registers {@link ArgumentType}
     *
     * @param type     the class-valueType of value being resolved from context
     * @param resolver the resolver for this value
     * @param <T>      the valueType of value being resolved from context
     */
    <T> void registerArgType(Type type, @NotNull ArgumentType<S, T> resolver);

    /**
     * Registers a custom {@link ArgumentTypeHandler}.
     * <p>
     * The handler will be added to the priority list and checked during type resolution
     * based on its priority.
     * </p>
     *
     * @param handler the handler to register
     */
    void registerArgTypeHandler(@NotNull ArgumentTypeHandler<S> handler);

    /**
     * Retrieves the {@link ArgumentTypeRegistry} associated with this registrar.
     *
     * @return the {@link ArgumentTypeRegistry} instance
     */
    ArgumentTypeRegistry<S> getArgumentTypeRegistry();

    /**
     * Retrieves the default suggestion resolver associated with this registrar.
     *
     * @return the {@link SuggestionResolver} instance used as the default resolver
     */
    SuggestionResolver<S> getDefaultSuggestionResolver();

    /**
     * Sets the default suggestion resolver to be used when no specific
     * suggestion resolver is provided. The default suggestion resolver
     * handles the auto-completion of arguments/parameters for commands.
     *
     * @param defaultSuggestionResolver the {@link SuggestionResolver} to be set as default
     */
    void setDefaultSuggestionResolver(SuggestionResolver<S> defaultSuggestionResolver);

    /**
     * Fetches the suggestion provider/resolver for a specific valueType of
     * argument or parameter.
     *
     * @param parameter the parameter symbolizing the valueType and argument name
     * @return the {@link SuggestionResolver} instance for that valueType
     */
    @SuppressWarnings("uncecked")
    default @NotNull SuggestionResolver<S> getParameterSuggestionResolver(Argument<S> parameter) {
        SuggestionResolver<S> parameterSpecificResolver = parameter.getSuggestionResolver();
        //ImperatDebugger.debug("Getting the suggestion resolver for param '%s'", parameter.format());
        if (parameterSpecificResolver == null) {
            var resolverByType = parameter.type().getSuggestionResolver();
            return Objects.requireNonNullElseGet(resolverByType, this::getDefaultSuggestionResolver);
        } else {
            return parameterSpecificResolver;
        }
    }

    /**
     * Fetches the suggestion provider/resolver for a specific valueType of
     * argument or parameter.
     *
     * @param type the valueType
     * @return the {@link SuggestionResolver} instance for that valueType
     */
    @Nullable
    SuggestionResolver<S> getSuggestionResolverByType(Type type);

    /**
     * Fetches the suggestion provider/resolver registered by its unique name
     *
     * @param name the name of the argument
     * @return the {@link SuggestionResolver} instance for that argument
     */
    @Nullable
    SuggestionResolver<S> getNamedSuggestionResolver(String name);


    /**
     * Registers a suggestion resolver
     *
     * @param name               the name of the suggestion resolver
     * @param suggestionResolver the suggestion resolver to register
     */
    void registerNamedSuggestionResolver(String name, SuggestionResolver<S> suggestionResolver);

    /**
     * Fetches the {@link SourceResolver} from an internal registry.
     *
     * @param type the target source valueType
     * @param <R>  the new source valueType parameter
     * @return the {@link SourceResolver} for specific valueType
     */
    <R> @Nullable SourceResolver<S, R> getSourceResolver(Type type);

    /**
     * Registers the {@link SourceResolver} into an internal registry
     *
     * @param type           the target source valueType
     * @param sourceResolver the source resolver to register
     * @param <R>            the new source valueType parameter
     */
    default <R> void registerSourceResolver(TypeWrap<R> type, SourceResolver<S, R> sourceResolver) {
        registerSourceResolver(type.getType(), sourceResolver);
    }

    /**
     * Registers the {@link SourceResolver} into an internal registry
     *
     * @param type           the target source valueType
     * @param sourceResolver the source resolver to register
     * @param <R>            the new source valueType parameter
     */
    <R> void registerSourceResolver(Type type, SourceResolver<S, R> sourceResolver);

    /**
     * Fetches the {@link ReturnResolver} from an internal registry.
     *
     * @param type the target type
     * @return the {@link ReturnResolver} for specific type
     */
    <T> @Nullable ReturnResolver<S, T> getReturnResolver(Type type);

    /**
     * Registers the {@link ReturnResolver} into an internal registry
     *
     * @param type           the target type
     * @param returnResolver the return resolver to register
     */
    default <T> void registerReturnResolver(TypeWrap<T> type, ReturnResolver<S, T> returnResolver) {
        registerReturnResolver(type.getType(), returnResolver);
    }

    /**
     * Registers the {@link ReturnResolver} into an internal registry
     *
     * @param type           the target type
     * @param returnResolver the return resolver to register
     */
    <T> void registerReturnResolver(Type type, ReturnResolver<S, T> returnResolver);

    /**
     * Registers a placeholder
     *
     * @param placeholder to register
     */
    void registerPlaceholder(Placeholder<S> placeholder);


}
