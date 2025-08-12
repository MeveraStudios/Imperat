package studio.mevera.imperat.command.tree.help.renderers.layouts;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.tree.help.HelpEntry;
import studio.mevera.imperat.command.tree.help.HelpEntryList;
import studio.mevera.imperat.command.tree.help.HelpRenderOptions;
import studio.mevera.imperat.command.tree.help.renderers.HelpDataTransformer;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;

import java.util.ArrayList;
import java.util.List;

final class ListTransformer<S extends Source>
        implements HelpDataTransformer<S, ListViewModel<S>> {
    
    @Override
    public @NotNull ListViewModel<S> transform(@NotNull ExecutionContext<S> context, @NotNull HelpEntryList<S> entries, @NotNull HelpRenderOptions<S> options) {
        List<ListViewModel.ListItem<S>> items = new ArrayList<>();
        
        for (HelpEntry<S> entry : entries) {
            ListViewModel.ListItem<S> item = new ListViewModel.ListItem<>(entry.getNode(), entry.getPathway());
            items.add(item);
        }
        
        return new ListViewModel<>(items);
    }
}