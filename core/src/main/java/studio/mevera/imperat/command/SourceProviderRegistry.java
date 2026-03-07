package studio.mevera.imperat.command;

import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.providers.SourceProvider;
import studio.mevera.imperat.util.Registry;

import java.lang.reflect.Type;

public final class SourceProviderRegistry<S extends CommandSource> extends Registry<Type, SourceProvider<S, ?>> {

    SourceProviderRegistry() {

    }

    public static <S extends CommandSource> SourceProviderRegistry<S> createDefault() {
        return new SourceProviderRegistry<>();
    }

}
