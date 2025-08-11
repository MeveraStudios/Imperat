package studio.mevera.imperat.command.tree.help.renderers;

import studio.mevera.imperat.command.tree.help.HelpEntryList;
import studio.mevera.imperat.command.tree.help.HelpRenderOptions;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;

/**
 * Base interface for layout-specific renderers.
 * Each layout works with its own data model.
 */
public interface HelpLayoutRenderer<S extends Source, T> {
    void render(
            ExecutionContext<S> context,
            T data,
            HelpEntryList<S> helpEntries,
            HelpRenderOptions options
    );
}