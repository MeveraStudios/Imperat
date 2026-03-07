package studio.mevera.imperat.command;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.returns.ReturnResolver;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.util.Registry;

import java.lang.reflect.Type;

@ApiStatus.AvailableSince("1.9.1")
public final class ReturnResolverRegistry<S extends CommandSource> extends Registry<Type, ReturnResolver<S, ?>> {

    public static <S extends CommandSource> ReturnResolverRegistry<S> createDefault() {
        return new ReturnResolverRegistry<>();
    }

    @SuppressWarnings("unchecked")
    public <T> @Nullable ReturnResolver<S, T> getReturnResolver(Type type) {
        return (ReturnResolver<S, T>) getData(type).orElse(null);
    }

}
