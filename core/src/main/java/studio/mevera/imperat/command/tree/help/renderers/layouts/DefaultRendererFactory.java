package studio.mevera.imperat.command.tree.help.renderers.layouts;

import studio.mevera.imperat.command.tree.help.HelpRenderOptions;
import studio.mevera.imperat.command.tree.help.renderers.HelpLayoutRenderer;
import studio.mevera.imperat.context.Source;

final class DefaultRendererFactory<S extends Source> extends HelpLayoutRendererFactory<S> {
    
    //lazy singleton pattern
    private static DefaultRendererFactory<?> instance;
    
    @SuppressWarnings("unchecked")
    static <S extends Source> HelpLayoutRendererFactory<S> getInstance() {
        
        if(instance == null) {
            instance = new DefaultRendererFactory<>();
        }
        return (HelpLayoutRendererFactory<S>) instance;
    }
    
    @Override
    public <C> HelpLayoutRenderer<S, C> create(HelpRenderOptions<S, C> options) {
        if(options.getLayout() == HelpRenderOptions.Layout.TREE) {
            return new TreeHelpRenderer<>();
        }
        else if(options.getLayout() == HelpRenderOptions.Layout.LIST) {
            // return new FlatHelpRenderer<>();
            return new ListHelpLayoutRenderer<>();
        }
        
        throw new IllegalArgumentException("Unsupported layout: " + options.getLayout());
    }
}
