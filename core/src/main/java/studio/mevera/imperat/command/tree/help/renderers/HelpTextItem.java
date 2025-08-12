package studio.mevera.imperat.command.tree.help.renderers;

import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;

/**
 * A functional interface for dynamically generating a help message string.
 * <p>
 * This interface extends {@link HelpItemFunc}, specializing it to produce a {@code String}
 * based on the provided {@link ExecutionContext}. It is marked as a {@code @FunctionalInterface}
 * because it defines a single abstract method, {@code load}, which allows it to be used
 * with lambda expressions and method references.
 *
 * @param <S> The type of {@link Source} from which the command was executed.
 * @see HelpItemFunc
 */
@FunctionalInterface
public interface HelpTextItem<S extends Source> extends HelpItemFunc<S, String> {

}
