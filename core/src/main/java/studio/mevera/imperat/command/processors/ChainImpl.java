package studio.mevera.imperat.command.processors;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.context.Source;

import java.util.Iterator;
import java.util.Queue;

record ChainImpl<S extends Source, P extends CommandProcessor<S>>(Queue<P> processors) implements CommandProcessingChain<S, P> {
    @Override
    public @NotNull Queue<P> getProcessors() {
        return processors;
    }

    @Override
    public void reset() {
        processors.clear();
    }

    @Override
    public void add(P processor) {
        processors.add(processor);
    }

    /**
     * Returns an iterator over elements of type {@code T}.
     *
     * @return an Iterator.
     */
    @Override
    public @NotNull Iterator<P> iterator() {
        return processors.iterator();
    }
}
