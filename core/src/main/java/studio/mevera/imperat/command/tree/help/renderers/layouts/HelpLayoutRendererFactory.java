package studio.mevera.imperat.command.tree.help.renderers.layouts;

import studio.mevera.imperat.command.tree.help.HelpRenderOptions;
import studio.mevera.imperat.command.tree.help.renderers.HelpLayoutRenderer;
import studio.mevera.imperat.context.Source;

public abstract class HelpLayoutRendererFactory<S extends Source> {
    
    public static <S extends Source> HelpLayoutRendererFactory<S> defaultFactory() {
        return DefaultRendererFactory.getInstance();
    }
    
    public abstract <C> HelpLayoutRenderer<S, C> create(HelpRenderOptions<S, C> options);
}
