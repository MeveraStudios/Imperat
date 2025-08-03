package studio.mevera.imperat.command;

import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.resolvers.SourceResolver;
import studio.mevera.imperat.util.Registry;

import java.lang.reflect.Type;

public final class SourceResolverRegistry<S extends Source> extends Registry<Type, SourceResolver<S, ?>> {

    SourceResolverRegistry() {

    }

    public static <S extends Source> SourceResolverRegistry<S> createDefault() {
        return new SourceResolverRegistry<>();
    }

}
