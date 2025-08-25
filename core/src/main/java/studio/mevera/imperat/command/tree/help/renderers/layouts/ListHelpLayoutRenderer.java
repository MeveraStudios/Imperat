package studio.mevera.imperat.command.tree.help.renderers.layouts;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.tree.help.*;
import studio.mevera.imperat.command.tree.help.renderers.HelpLayoutRenderer;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;

final class ListHelpLayoutRenderer<S extends Source, C>
        implements HelpLayoutRenderer<S, C> {
    
    @Override
    public void render(
            @NotNull ExecutionContext<S> context,
            @NotNull HelpEntryList<S> helpEntries,
            @NotNull HelpRenderOptions<S, C> options
    ) {
        S source = context.source();
        
        for (HelpEntry<S> entry : helpEntries) {
            
            HelpComponent<C> component = options.getTheme()
                    .getUsageDecorator().decorate(
                        context.command(),
                        entry.getPathway(),
                        context,
                        options
                    );
            
            component.send(source);
        }
    }
    
}