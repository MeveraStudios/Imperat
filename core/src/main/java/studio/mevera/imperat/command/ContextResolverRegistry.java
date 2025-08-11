package studio.mevera.imperat.command;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.annotations.base.element.ParameterElement;
import studio.mevera.imperat.context.ArgumentInput;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.resolvers.ContextResolver;
import studio.mevera.imperat.util.Registry;
import studio.mevera.imperat.util.TypeWrap;

import java.lang.reflect.Type;
import java.util.Optional;

@ApiStatus.AvailableSince("1.0.0")
public final class ContextResolverRegistry<S extends Source> extends Registry<Type, ContextResolver<S, ?>> {

    private final Registry<Type, ContextResolverFactory<S, ?>> factories = new Registry<>();

    private ContextResolverRegistry() {
        super();
        //this.registerResolver(TypeWrap.of(CommandHelp.class).getType(), (ctx, param) -> new CommandHelp(config, ctx));
        this.registerResolver(new TypeWrap<ExecutionContext<S>>(){}.getType(), (ctx, param)-> ctx);
        this.registerResolver(new TypeWrap<ArgumentInput>(){}.getType(),(ctx, param)-> ctx.arguments());
    }

    public static <S extends Source> ContextResolverRegistry<S> createDefault() {
        return new ContextResolverRegistry<>();
    }

    public <T> void registerResolver(Type type, ContextResolver<S, T> resolver) {
        setData(type, resolver);
    }

    public <T> void registerFactory(Type type, ContextResolverFactory<S, T> factory) {
        factories.setData(type, factory);
    }

    public Optional<ContextResolverFactory<S, ?>> getFactoryFor(Type type) {
        return factories.getData(type);
    }

    @SuppressWarnings("unchecked")
    public <T> @Nullable ContextResolver<S, T> getContextResolver(Type type, @Nullable ParameterElement element) {
        //we search for factories mainly
        ContextResolverFactory<S, T> factory = (ContextResolverFactory<S, T>) getFactoryFor(type).orElse(null);
        if (factory == null) {
            return factories.getData(type)
                .map((defaultFactory) -> ((ContextResolverFactory<S, T>) defaultFactory).create(type, element))
                .orElse((ContextResolver<S, T>) getData(type).orElse(null));
        }
        return factory.create(type, element);
    }

    public <T> @Nullable ContextResolver<S, T> getResolverWithoutParameterElement(Type type) {
        return getContextResolver(type, null);
    }

}
