package studio.mevera.imperat;

import net.minestom.server.command.builder.exception.ArgumentSyntaxException;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.CommandException;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.function.BiFunction;

public final class MinestomArgumentType<T> extends ArgumentType<MinestomCommandSource, T> {

    private final BiFunction<Type, String, net.minestom.server.command.builder.arguments.Argument<?>> minestomType;
    private final int numberOfParametersToConsume;
    public MinestomArgumentType(
            Type type,
            BiFunction<Type, String, net.minestom.server.command.builder.arguments.Argument<?>> minestomType,
            int numberOfParametersToConsume
    ) {
        super(type);
        this.minestomType = minestomType;
        this.numberOfParametersToConsume = numberOfParametersToConsume;
    }

    //for greedy arguments.
    public MinestomArgumentType(
            Type type,
            BiFunction<Type, String, net.minestom.server.command.builder.arguments.Argument<?>> minestomType    ) {
        super(type);
        this.minestomType = minestomType;
        this.numberOfParametersToConsume = -1;
    }

    @Override
    public T parse(@NotNull CommandContext<MinestomCommandSource> context, @NonNull Argument<MinestomCommandSource> argument, @NotNull String input)
            throws CommandException {
        try {
            return (T) getMinestomType("").parse(context.source().origin(), input);
        } catch (ArgumentSyntaxException exception) {
            throw new CommandException(exception.getMessage());
        }
    }

    @Override @SuppressWarnings("unchecked")
    public @NotNull T parse(
            @NotNull ExecutionContext<MinestomCommandSource> context,
            @NotNull Cursor<MinestomCommandSource> cursor
    ) throws CommandException {
        String correspondingInput = cursor.currentRawIfPresent();
        int limit = numberOfParametersToConsume == -1 ? cursor.rawsLength()-1 : numberOfParametersToConsume;
        //greedy
        assert correspondingInput != null;
        StringBuilder input = new StringBuilder(correspondingInput);
        for (int i = 1; i <= limit; i++) {
            cursor.currentRaw().ifPresent(raw -> {
                input.append(" ").append(raw);
                cursor.skipRaw();
            });
        }

        try {
            return (T) getMinestomType(
                    Objects.requireNonNull(cursor.currentParameterIfPresent()).getName()
            ).parse(
                    context.source().origin(),
                    input.toString()
            );
        }catch (ArgumentSyntaxException exception) {
            throw new CommandException(exception.getMessage());
        }
    }

    @Override
    public int getNumberOfParametersToConsume(Argument<MinestomCommandSource> argument) {
        return numberOfParametersToConsume;
    }

    @Override
    public boolean isGreedy(Argument<MinestomCommandSource> parameter) {
        return numberOfParametersToConsume == -1;
    }

    public net.minestom.server.command.builder.arguments.Argument<?> getMinestomType(String name) {
        return minestomType.apply(type, name);
    }
}
