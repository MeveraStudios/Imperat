package studio.mevera.imperat;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.ContextArgumentProviderFactory;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.command.parameters.type.ArgumentType;
import studio.mevera.imperat.command.parameters.type.ArgumentTypeHandler;
import studio.mevera.imperat.command.returns.ReturnResolver;
import studio.mevera.imperat.context.ArgumentTypeRegistry;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.placeholders.Placeholder;
import studio.mevera.imperat.providers.ContextArgumentProvider;
import studio.mevera.imperat.providers.SourceProvider;
import studio.mevera.imperat.providers.SuggestionProvider;
import studio.mevera.imperat.responses.ResponseRegistry;
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
     * Retrieves the {@link ResponseRegistry} associated with this registrar.
     *
     * @return the {@link ResponseRegistry} instance
     */
    @NotNull ResponseRegistry getResponseRegistry();

    /**
     * Registers a context resolver factory
     *
     * @param factory the factory to register
     */
    <T> void registerContextResolverFactory(Type type, ContextArgumentProviderFactory<S, T> factory);


    /**
     * Registers {@link ContextArgumentProvider}
     *
     * @param type     the class-valueType of value being resolved from context
     * @param resolver the resolver for this value
     * @param <T>      the valueType of value being resolved from context
     */
    <T> void registerContextResolver(Type type, @NotNull ContextArgumentProvider<S, T> resolver);


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
     * @return the {@link SuggestionProvider} instance used as the default resolver
     */
    SuggestionProvider<S> getDefaultSuggestionResolver();

    /**
     * Sets the default suggestion resolver to be used when no specific
     * suggestion resolver is provided. The default suggestion resolver
     * handles the auto-completion of arguments/parameters for commands.
     *
     * @param defaultSuggestionProvider the {@link SuggestionProvider} to be set as default
     */
    void setDefaultSuggestionResolver(SuggestionProvider<S> defaultSuggestionProvider);

    /**
     * Fetches the suggestion provider/resolver for a specific valueType of
     * argument or parameter.
     *
     * @param parameter the parameter symbolizing the valueType and argument name
     * @return the {@link SuggestionProvider} instance for that valueType
     */
    @SuppressWarnings("uncecked")
    default @NotNull SuggestionProvider<S> getParameterSuggestionResolver(Argument<S> parameter) {
        SuggestionProvider<S> parameterSpecificResolver = parameter.getSuggestionResolver();
        //ImperatDebugger.debug("Getting the suggestion resolver for param '%s'", parameter.format());
        if (parameterSpecificResolver == null) {
            var resolverByType = parameter.type().getSuggestionProvider();
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
     * @return the {@link SuggestionProvider} instance for that valueType
     */
    @Nullable
    SuggestionProvider<S> getSuggestionProviderForType(Type type);


    /**
     * Fetches the {@link SourceProvider} from an internal registry.
     *
     * @param type the target source valueType
     * @param <R>  the new source valueType parameter
     * @return the {@link SourceProvider} for specific valueType
     */
    <R> @Nullable SourceProvider<S, R> getSourceProviderFor(Type type);

    /**
     * Registers the {@link SourceProvider} into an internal registry
     *
     * @param type           the target source valueType
     * @param sourceProvider the source resolver to register
     * @param <R>            the new source valueType parameter
     */
    default <R> void registerSourceProvider(TypeWrap<R> type, SourceProvider<S, R> sourceProvider) {
        registerSourceProvider(type.getType(), sourceProvider);
    }

    /**
     * Registers the {@link SourceProvider} into an internal registry
     *
     * @param type           the target source valueType
     * @param sourceProvider the source resolver to register
     * @param <R>            the new source valueType parameter
     */
    <R> void registerSourceProvider(Type type, SourceProvider<S, R> sourceProvider);

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
    void registerPlaceholder(Placeholder placeholder);


}
