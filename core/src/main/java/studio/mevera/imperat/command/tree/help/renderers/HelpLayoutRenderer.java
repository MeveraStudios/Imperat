package studio.mevera.imperat.command.tree.help.renderers;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.tree.help.HelpEntryList;
import studio.mevera.imperat.command.tree.help.HelpTheme;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;

/**
 * Interface for rendering help documentation.
 * Simplified to use HelpTheme directly instead of HelpRenderOptions.
 *
 * @param <S> the source type
 * @param <C> the component type
 */
public interface HelpLayoutRenderer<S extends Source, C> {
    
    /**
     * Renders the help documentation to the source.
     *
     * @param context The execution context, containing information about the command and source.
     * @param helpEntries A list of all available help entries for the current context.
     * @param theme The theme that controls how the help is displayed.
     */
    void render(
            @NotNull ExecutionContext<S> context,
            @NotNull HelpEntryList<S> helpEntries,
            @NotNull HelpTheme<S, C> theme
    );
}