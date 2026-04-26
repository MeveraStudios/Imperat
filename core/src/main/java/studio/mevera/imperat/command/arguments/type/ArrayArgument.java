package studio.mevera.imperat.command.arguments.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.util.TypeWrap;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public abstract class ArrayArgument<S extends CommandSource, E> extends GreedyArgumentType<S, E[]> {

    private final Function<Integer, Object[]> initializer;
    private final ArgumentType<S, E> componentType;

    public ArrayArgument(TypeWrap<E[]> type, Function<Integer, Object[]> initializer, ArgumentType<S, E> componentType) {
        super(type.getType());
        this.initializer = initializer;
        this.componentType = componentType;
    }

    @Override
    @SuppressWarnings("unchecked")
    public E[] parse(@NotNull CommandContext<S> context, @NotNull Argument<S> argument, @NotNull String joinedInput) throws CommandException {
        if (joinedInput.isBlank()) {
            return (E[]) initializer.apply(0);
        }

        String[] raws = joinedInput.split(" ");
        List<E> elements = new ArrayList<>(raws.length);
        for (String raw : raws) {
            elements.add(componentType.parse(context, argument, Cursor.single(context, raw)));
        }

        E[] array = (E[]) initializer.apply(elements.size());
        return elements.toArray(array);
    }
}
