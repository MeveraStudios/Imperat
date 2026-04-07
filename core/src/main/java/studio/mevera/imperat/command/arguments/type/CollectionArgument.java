package studio.mevera.imperat.command.arguments.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.util.TypeWrap;

import java.util.Collection;
import java.util.function.Supplier;

public class CollectionArgument<S extends CommandSource, E, C extends Collection<E>> extends ArgumentType<S, C> {

    private final Supplier<C> collectionSupplier;
    private final ArgumentType<S, E> componentResolver;

    public CollectionArgument(TypeWrap<C> type, Supplier<C> collectionSupplier, ArgumentType<S, E> componentResolver) {
        super(type.getType());
        this.collectionSupplier = collectionSupplier;
        this.componentResolver = componentResolver;
    }

    @Override
    public C parse(@NotNull CommandContext<S> context, @NotNull Argument<S> argument, @NotNull String input) throws CommandException {
        String[] raws = input.split(" ");
        C newCollection = collectionSupplier.get();
        for (String raw : raws) {
            E value = componentResolver.parse(context, argument, raw);
            newCollection.add(value);
        }

        return newCollection;
    }

    @Override
    public boolean isGreedy(Argument<S> parameter) {
        return true;
    }
}
