package studio.mevera.imperat.paper.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

final class PaperArgumentTypeImpl<N, T> implements PaperArgumentType<N, T> {

    private final ArgumentType<N> nativeType;
    private final Function<N, T> resolver;

    PaperArgumentTypeImpl(@NotNull ArgumentType<N> nativeType, @NotNull Function<N, T> resolver) {
        this.nativeType = nativeType;
        this.resolver = resolver;
    }

    @Override
    public @NotNull ArgumentType<N> nativeType() {
        return nativeType;
    }

    @Override
    public @NotNull Function<N, T> resolver() {
        return resolver;
    }
}
