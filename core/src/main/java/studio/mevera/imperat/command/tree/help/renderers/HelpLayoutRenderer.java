package studio.mevera.imperat.command.tree.help.renderers;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.tree.help.HelpComponent;
import studio.mevera.imperat.command.tree.help.HelpEntryList;
import studio.mevera.imperat.command.tree.help.HelpRenderOptions;
import studio.mevera.imperat.command.tree.help.HelpTheme;
import studio.mevera.imperat.command.tree.help.renderers.layouts.UsageDecorator;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;

public interface HelpLayoutRenderer<S extends Source, C> {
    
    
    /**
     * Renders the help documentation to the source.
     *
     * @param context The execution context, containing information about the command and source.
     * @param helpEntries A list of all available help entries for the current context.
     * @param options The rendering options that may influence how the data is displayed.
     */
    void render(
            @NotNull ExecutionContext<S> context,
            @NotNull HelpEntryList<S> helpEntries,
            @NotNull HelpRenderOptions<S, C> options
    );
}