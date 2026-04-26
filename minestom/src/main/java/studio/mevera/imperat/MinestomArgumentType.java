package studio.mevera.imperat;

import net.minestom.server.command.builder.exception.ArgumentSyntaxException;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.command.arguments.type.Cursor;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.exception.CommandException;

import java.lang.reflect.Type;
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
    @SuppressWarnings("unchecked")
    public T parse(@NotNull CommandContext<MinestomCommandSource> context, @NonNull Argument<MinestomCommandSource> argument,
            @NotNull Cursor<MinestomCommandSource> cursor)
            throws CommandException {
        // Drain the cursor's full budget — fixed-arity Minestom types declare
        // their token count via {@link #getNumberOfParametersToConsume},
        // greedy ones use {@code -1} (unbounded). Either way, joining the
        // budget tokens with a single space produces the input Minestom's
        // own parser expects.
        String input = cursor.collectRemaining();
        try {
            return (T) getMinestomType(argument.getName()).parse(context.source().origin(), input);
        } catch (ArgumentSyntaxException exception) {
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
