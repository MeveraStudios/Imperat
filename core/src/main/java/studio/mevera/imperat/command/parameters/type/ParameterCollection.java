package studio.mevera.imperat.command.parameters.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.util.TypeWrap;

import java.util.Collection;
import java.util.function.Supplier;

public class ParameterCollection<S extends Source, E, C extends Collection<E>> extends BaseParameterType<S, C> {

    private final Supplier<C> collectionSupplier;
    private final ParameterType<S, E> componentResolver;

    public ParameterCollection(TypeWrap<C> type, Supplier<C> collectionSupplier, ParameterType<S, E> componentResolver) {
        super(type.getType());
        this.collectionSupplier = collectionSupplier;
        this.componentResolver = componentResolver;
    }

    @Override
    public @Nullable C resolve(@NotNull ExecutionContext<S> context, @NotNull CommandInputStream<S> commandInputStream, @NotNull String input) throws
            CommandException {
        C newCollection = collectionSupplier.get();

        while (commandInputStream.isCurrentRawInputAvailable()) {

            String raw = commandInputStream.currentRaw().orElse(null);
            if (raw == null) {
                break;
            }

            E element = componentResolver.resolve(context, CommandInputStream.subStream(commandInputStream, raw), raw);
            newCollection.add(element);

            commandInputStream.skipRaw();
        }
        return newCollection;
    }

    @Override
    public boolean isGreedy(CommandParameter<S> parameter) {
        return true;
    }
}
