package studio.mevera.imperat.command.tree.help.renderers;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.tree.help.HelpEntryList;
import studio.mevera.imperat.command.tree.help.HelpRenderOptions;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;

/**
 * Base interface for layout-specific renderers.
 * <p>
 * This interface defines the contract for components responsible for rendering
 * help documentation based on a specific data model. Each implementation
 * of this interface is tied to a particular data model (`T`) and is responsible
 * for presenting that data to the user in a consistent format.
 *
 * @param <S> The type of {@link Source} from which the command was executed.
 * @param <T> The type of the layout-specific data model to be rendered.
 */
@FunctionalInterface
public interface HelpLayoutRenderer<S extends Source, T> {
    
    /**
     * Renders the help documentation to the source.
     *
     * @param context The execution context, containing information about the command and source.
     * @param data The layout-specific data model to be rendered.
     * @param helpEntries A list of all available help entries for the current context.
     * @param options The rendering options that may influence how the data is displayed.
     */
    void render(
            @NotNull ExecutionContext<S> context,
            @NotNull T data,
            @NotNull HelpEntryList<S> helpEntries,
            @NotNull HelpRenderOptions<S> options
    );
}