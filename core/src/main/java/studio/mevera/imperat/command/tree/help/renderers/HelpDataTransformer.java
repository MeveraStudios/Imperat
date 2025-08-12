package studio.mevera.imperat.command.tree.help.renderers;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.tree.help.HelpEntryList;
import studio.mevera.imperat.command.tree.help.HelpRenderOptions;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;

/**
 * A functional interface for a transformer that converts raw help entries into a layout-specific data structure.
 * <p>
 * This interface is designed to be a core component of a flexible help system, allowing
 * different help layouts (e.g., flat list, nested tree) to be generated from the same
 * raw data. Implementations of this interface will take a list of raw help entries and
 * a rendering context to produce a specific, display-ready data model.
 *
 * @param <S> The type of {@link Source} from which the command was executed.
 * @param <T> The type of the layout-specific data structure produced by the transformer.
 */
@FunctionalInterface
public interface HelpDataTransformer<S extends Source, T> {
    
    /**
     * Transforms a list of raw help entries into a layout-specific data structure.
     *
     * @param context The execution context.
     * @param entries The raw list of help entries to be transformed.
     * @param options The rendering options that may influence the transformation.
     * @return A data structure of type T, formatted for a specific help layout.
     */
    @NotNull T transform(
            @NotNull ExecutionContext<S> context,
            @NotNull HelpEntryList<S> entries,
            @NotNull HelpRenderOptions<S> options
    );
    
}
