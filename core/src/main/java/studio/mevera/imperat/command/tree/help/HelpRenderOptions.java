package studio.mevera.imperat.command.tree.help;

/**
 * Rendering options for customizing help display.
 */
public class HelpRenderOptions {
    private int page = 1;
    private int pageSize = 10;
    private HelpTheme theme = HelpTheme.defaultTheme();
    private Layout layout = Layout.LIST;
    
    public enum Layout {
        LIST,       // Simple list view
        TREE,       // Tree structure view
    }
    
    public int getPage() {
        return page;
    }
    
    public int getPageSize() {
        return pageSize;
    }
    
    public HelpTheme getTheme() {
        return theme;
    }
    
    public Layout getLayout() {
        return layout;
    }
    
    public HelpRenderOptions page(int page) {
        this.page = page;
        return this;
    }
    
    public HelpRenderOptions pageSize(int pageSize) {
        this.pageSize = pageSize;
        return this;
    }
    
    public HelpRenderOptions theme(HelpTheme theme) {
        this.theme = theme;
        return this;
    }
    
    public HelpRenderOptions layout(Layout layout) {
        this.layout = layout;
        return this;
    }
    
    // Builder pattern for options...
}