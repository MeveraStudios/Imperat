package studio.mevera.imperat.paper.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

final class PaperArgumentTypeImpl<N, T> implements PaperArgumentType<N, T> {

    private final ArgumentType<N> nativeType;
    private final BiFunction<N, CommandSourceStack, T> resolver;

    PaperArgumentTypeImpl(@NotNull ArgumentType<N> nativeType, @NotNull BiFunction<N, CommandSourceStack, T> resolver) {
        this.nativeType = nativeType;
        this.resolver = resolver;
    }

    @Override
    public @NotNull ArgumentType<N> nativeType() {
        return nativeType;
    }

    @Override
    public @NotNull BiFunction<N, CommandSourceStack, T> resolver() {
        return resolver;
    }
}
