package studio.mevera.imperat.command;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.annotations.base.element.ParameterElement;
import studio.mevera.imperat.context.ArgumentInput;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.providers.ContextArgumentProvider;
import studio.mevera.imperat.util.Registry;
import studio.mevera.imperat.util.TypeWrap;

import java.lang.reflect.Type;
import java.util.Optional;

@ApiStatus.AvailableSince("1.0.0")
public final class ContextArgumentProviderRegistry<S extends Source> extends Registry<Type, ContextArgumentProvider<S, ?>> {

    private final Registry<Type, ContextArgumentProviderFactory<S, ?>> factories = new Registry<>();

    private ContextArgumentProviderRegistry() {
        super();
        //this.registerResolver(TypeWrap.of(CommandHelp.class).getType(), (ctx, param) -> new CommandHelp(config, ctx));
        this.registerResolver(new TypeWrap<ExecutionContext<S>>() {
        }.getType(), (ctx, param) -> ctx);
        this.registerResolver(new TypeWrap<ArgumentInput>() {
        }.getType(), (ctx, param) -> ctx.arguments());
    }

    public static <S extends Source> ContextArgumentProviderRegistry<S> createDefault() {
        return new ContextArgumentProviderRegistry<>();
    }

    public <T> void registerResolver(Type type, ContextArgumentProvider<S, T> resolver) {
        setData(type, resolver);
    }

    public <T> void registerFactory(Type type, ContextArgumentProviderFactory<S, T> factory) {
        factories.setData(type, factory);
    }

    public Optional<ContextArgumentProviderFactory<S, ?>> getFactoryFor(Type type) {
        return factories.getData(type);
    }

    @SuppressWarnings("unchecked")
    public <T> @Nullable ContextArgumentProvider<S, T> getContextResolver(Type type, @Nullable ParameterElement element) {
        //we search for factories mainly
        ContextArgumentProviderFactory<S, T> factory = (ContextArgumentProviderFactory<S, T>) getFactoryFor(type).orElse(null);
        if (factory == null) {
            return factories.getData(type)
                           .map((defaultFactory) -> ((ContextArgumentProviderFactory<S, T>) defaultFactory).create(type, element))
                           .orElse((ContextArgumentProvider<S, T>) getData(type).orElse(null));
        }
        return factory.create(type, element);
    }

    public <T> @Nullable ContextArgumentProvider<S, T> getResolverWithoutParameterElement(Type type) {
        return getContextResolver(type, null);
    }

}
