package studio.mevera.imperat.command.tree.help.renderers.list;

import java.util.List;

public class ListViewModel {
    public record ListItem(String command, String signature, String description, String permission) { }
    
    public final List<ListItem> items;
    
    public ListViewModel(List<ListItem> items) {
        this.items = items;
    }
}
