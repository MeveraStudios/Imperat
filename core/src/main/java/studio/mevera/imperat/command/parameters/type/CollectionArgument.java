package studio.mevera.imperat.command.parameters.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.util.TypeWrap;

import java.util.Collection;
import java.util.function.Supplier;

public class CollectionArgument<S extends Source, E, C extends Collection<E>> extends ArgumentType<S, C> {

    private final Supplier<C> collectionSupplier;
    private final ArgumentType<S, E> componentResolver;

    public CollectionArgument(TypeWrap<C> type, Supplier<C> collectionSupplier, ArgumentType<S, E> componentResolver) {
        super(type.getType());
        this.collectionSupplier = collectionSupplier;
        this.componentResolver = componentResolver;
    }

    @Override
    public @Nullable C parse(@NotNull ExecutionContext<S> context, @NotNull Cursor<S> cursor, @NotNull String correspondingInput) throws
            CommandException {
        C newCollection = collectionSupplier.get();

        while (cursor.isCurrentRawInputAvailable()) {

            String raw = cursor.currentRaw().orElse(null);
            if (raw == null) {
                break;
            }

            E element = componentResolver.parse(context, Cursor.subStream(cursor, raw), raw);
            newCollection.add(element);

            cursor.skipRaw();
        }
        return newCollection;
    }

    @Override
    public boolean isGreedy(Argument<S> parameter) {
        return true;
    }
}
