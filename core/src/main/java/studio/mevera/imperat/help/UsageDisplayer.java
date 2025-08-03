package studio.mevera.imperat.help;

import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;

import java.util.Collection;

@FunctionalInterface
public interface UsageDisplayer<S extends Source> {

    void accept(ExecutionContext<S> ctx, S source, Collection<? extends CommandUsage<S>> commandUsages);

}
