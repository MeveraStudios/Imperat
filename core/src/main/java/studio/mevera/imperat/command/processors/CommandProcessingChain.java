package studio.mevera.imperat.command.processors;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.context.Source;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;

public interface CommandProcessingChain<S extends Source, P extends CommandProcessor<S>> extends Iterable<P> {

    static <S extends Source> Builder<S, CommandPreProcessor<S>> preProcessors() {
        return new Builder<>();
    }

    static <S extends Source> Builder<S, CommandPostProcessor<S>> postProcessors() {
        return new Builder<>();
    }

    @NotNull
    Queue<P> getProcessors();

    void reset();

    void add(P preProcessor);

    final class Builder<S extends Source, P extends CommandProcessor<S>> {

        private final PriorityQueue<P> processors;

        public Builder() {
            // Create a priority queue that sorts by the priority of the processors
            this.processors = new PriorityQueue<>(Comparator.comparingInt(CommandProcessor::priority));
        }

        public Builder<S, P> then(P processor) {
            // Add the processor to the queue
            processors.offer(processor);
            return this;
        }

        public CommandProcessingChain<S, P> build() {
            return new ChainImpl<>(processors);
        }
    }

}
