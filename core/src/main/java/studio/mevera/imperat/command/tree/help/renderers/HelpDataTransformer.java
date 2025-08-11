package studio.mevera.imperat.command.tree.help.renderers;

import studio.mevera.imperat.command.tree.help.HelpEntryList;
import studio.mevera.imperat.command.tree.help.HelpRenderOptions;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;

/**
 * Transforms raw help entries into a layout-specific data structure.
 * Each layout has its own transformer and data model.
 */
public interface HelpDataTransformer<S extends Source, T> {
    T transform(ExecutionContext<S> context, HelpEntryList<S> entries, HelpRenderOptions options);
}