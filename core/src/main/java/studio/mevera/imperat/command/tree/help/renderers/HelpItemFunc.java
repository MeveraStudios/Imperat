package studio.mevera.imperat.command.tree.help.renderers;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;

/**
 * A functional interface for a function that loads an item of a specific type.
 * <p>
 * This is a generic functional interface that provides a method to load an item
 * of type {@code T} based on a given {@link ExecutionContext}. It is designed
 * to be implemented with lambda expressions for concise and dynamic content generation.
 *
 * @param <S> The type of {@link Source} from which the command was executed.
 * @param <T> The type of the item to be loaded.
 */
@FunctionalInterface
public interface HelpItemFunc<S extends Source, T> {
    
    /**
     * Loads an item of type {@code T} based on the execution context.
     *
     * @param context The execution context.
     * @return The loaded item.
     */
    @NotNull T load(@NotNull ExecutionContext<S> context);
}