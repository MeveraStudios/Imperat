package studio.mevera.imperat.command.tree.help.renderers.list;

import studio.mevera.imperat.command.tree.help.HelpEntry;
import studio.mevera.imperat.command.tree.help.HelpEntryList;
import studio.mevera.imperat.command.tree.help.HelpRenderOptions;
import studio.mevera.imperat.command.tree.help.renderers.HelpDataTransformer;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;

import java.util.ArrayList;
import java.util.List;

public class ListTransformer<S extends Source>
        implements HelpDataTransformer<S, ListViewModel> {
    
    @Override
    public ListViewModel transform(ExecutionContext<S> context, HelpEntryList<S> entries, HelpRenderOptions options) {
        List<ListViewModel.ListItem> items = new ArrayList<>();
        
        for (HelpEntry<S> entry : entries) {
            items.add(new ListViewModel.ListItem(
                entry.getPathway().formatted(),
                entry.getPathway().formatted(),
                entry.getPathway().description().toString(),
                entry.getNode().getPermission()
            ));
        }
        
        return new ListViewModel(items);
    }
}