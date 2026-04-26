package studio.mevera.imperat;

import net.minestom.server.command.builder.exception.ArgumentSyntaxException;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
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
    public T parse(@NotNull CommandContext<MinestomCommandSource> context, @NonNull Argument<MinestomCommandSource> argument, @NotNull String input)
            throws CommandException {
        // The command tree pre-joins the consumed tokens into {@code input}
        // based on {@link #getNumberOfParametersToConsume(Argument)} and
        // {@link #isGreedy(Argument)}, so Minestom receives the same string it
        // would have seen via the legacy cursor path.
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
