package studio.mevera.imperat.command.tree.help.renderers.layouts;

import studio.mevera.imperat.command.tree.help.HelpEntryList;
import studio.mevera.imperat.command.tree.help.HelpRenderOptions;
import studio.mevera.imperat.command.tree.help.renderers.HelpDataTransformer;
import studio.mevera.imperat.command.tree.help.renderers.HelpLayoutRenderer;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import java.util.EnumMap;
import java.util.Map;

/**
 * Orchestrates the overall rendering of help entries.
 * Handles pagination, themes, layouts, and formatting.
 * 
 * @param <S> the source type
 */
public class HelpLayoutRendererManager<S extends Source> {
    
    private final Map<HelpRenderOptions.Layout, LayoutPipeline<S, ?>> pipelines;
    
    public HelpLayoutRendererManager() {
        this.pipelines = new EnumMap<>(HelpRenderOptions.Layout.class);
        
        // Register layout pipelines
        registerPipeline(HelpRenderOptions.Layout.TREE,
                (ctx, entries, options)-> "", new HierarchicalTreeRenderer<>());
        registerPipeline(HelpRenderOptions.Layout.LIST,
                new ListTransformer<>(), new ListHelpLayoutRenderer<>());
        /*registerPipeline(HelpRenderOptions.Layout.COMPACT,
                new CompactTransformer<>(), new CompactLayoutRenderer<>());
        registerPipeline(HelpRenderOptions.Layout.DETAILED,
                new DetailedTransformer<>(), new DetailedLayoutRenderer<>());
                
         */
    }
    
    public <T> void registerPipeline(HelpRenderOptions.Layout layout,
                                      HelpDataTransformer<S, T> transformer,
                                      HelpLayoutRenderer<S, T> renderer) {
        pipelines.put(layout, new LayoutPipeline<>(transformer, renderer));
    }
    
    public void render(ExecutionContext<S> context, HelpEntryList<S> entries,
                       HelpRenderOptions<S> options) {
        LayoutPipeline<S, ?> pipeline = pipelines.get(options.getLayout());
        if (pipeline == null) {
            throw new IllegalArgumentException("Unknown layout: " + options.getLayout());
        }
        
        pipeline.execute(context, entries, options);
    }
    
    private record LayoutPipeline<S extends Source, T>(
            HelpDataTransformer<S, T> transformer,
            HelpLayoutRenderer<S, T> renderer
    ) {
        void execute(
                ExecutionContext<S> context,
                HelpEntryList<S> entries,
                HelpRenderOptions<S> options
        ) {
            // Transform
            T model = transformer.transform(context, entries, options);
            
            // Render with theme
            renderer.render(context, model, entries, options);
        }
    }
    
}