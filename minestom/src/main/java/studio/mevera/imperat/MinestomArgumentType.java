package studio.mevera.imperat;

import net.minestom.server.command.builder.exception.ArgumentSyntaxException;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.command.parameters.type.ArgumentType;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.CommandException;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.function.BiFunction;

public final class MinestomArgumentType<T> extends ArgumentType<MinestomSource, T> {

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

    @Override @SuppressWarnings("unchecked")
    public @NotNull T parse(
            @NotNull ExecutionContext<MinestomSource> context,
            @NotNull Cursor<MinestomSource> cursor,
            @NotNull String correspondingInput
    ) throws CommandException {

        int limit = numberOfParametersToConsume == -1 ? cursor.rawsLength()-1 : numberOfParametersToConsume;
        //greedy
        StringBuilder input = new StringBuilder(correspondingInput);
        for (int i = 1; i <= limit; i++) {
            cursor.currentRaw().ifPresent(raw -> {
                input.append(" ").append(raw);
                cursor.skipRaw();
            });
        }

        try {
            return (T) getMinestomType(
                    Objects.requireNonNull(cursor.currentParameterIfPresent()).name()
            ).parse(
                    context.source().origin(),
                    input.toString()
            );
        }catch (ArgumentSyntaxException exception) {
            throw new CommandException(exception.getMessage());
        }
    }

    @Override
    public boolean matchesInput(
            int rawPosition,
            Context<MinestomSource> context,
            Argument<MinestomSource> parameter
    ) {
        //collect input using the limit
        int limit = numberOfParametersToConsume == -1 ? context.arguments().size()-1 : numberOfParametersToConsume;
        StringBuilder input = new StringBuilder();
        for (int i = rawPosition; i <= limit; i++) {
            String arg = context.arguments().getOr(rawPosition + i, null);
            if (arg == null) {
                break;
            }
            if (i > 0) {
                input.append(" ");
            }
            input.append(arg);
        }
        try {
            getMinestomType(parameter.name()).parse(context.source().origin(), input.toString());
            return true;
        }catch (ArgumentSyntaxException ex) {
            return false;
        }
    }



    @Override
    public int getNumberOfParametersToConsume() {
        return numberOfParametersToConsume;
    }

    @Override
    public boolean isGreedy(Argument<MinestomSource> parameter) {
        return numberOfParametersToConsume == -1;
    }

    public net.minestom.server.command.builder.arguments.Argument<?> getMinestomType(String name) {
        return minestomType.apply(type, name);
    }
}
