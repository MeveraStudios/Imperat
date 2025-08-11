package studio.mevera.imperat.command.tree.help;

import studio.mevera.imperat.command.tree.help.renderers.HelpLayoutRendererManager;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;

/**
 * Main help coordinator that ties everything together.
 */
public class HelpCoordinator<S extends Source> {
    
    private final HelpProvider<S> provider;
    private final HelpLayoutRendererManager<S> renderer;
    
    public HelpCoordinator(HelpProvider<S> provider, HelpLayoutRendererManager<S> renderer) {
        this.provider = provider;
        this.renderer = renderer;
    }
    
    public void showHelp(ExecutionContext<S> context, HelpQuery<S> query, HelpRenderOptions options) {
        // Step 1: Get data
        HelpEntryList<S> entries = provider.provide(context.command(), query);
        
        // Step 2: Render the data
        this.renderer.render(context, entries, options);
    }
}