package studio.mevera.imperat.command.tree.help;

import studio.mevera.imperat.command.tree.help.renderers.HelpLayoutRenderer;
import studio.mevera.imperat.command.tree.help.renderers.layouts.HelpLayoutRendererFactory;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;


public class HelpCoordinator<S extends Source> {
    
    private final HelpProvider<S> provider;
    
    private final HelpLayoutRendererFactory<S> rendererFactory;
    
    /**
     * Constructs a new HelpCoordinator with a custom provider and renderer manager.
     *
     * @param provider The provider responsible for fetching help entries.
     * @param rendererFactory The renderer factory to create layout renderers.
     */
    private HelpCoordinator(
            HelpProvider<S> provider,
            HelpLayoutRendererFactory<S> rendererFactory
    ) {
        this.provider = provider;
        this.rendererFactory = rendererFactory;
    }
    
    /**
     * A factory method to create a new HelpCoordinator with custom settings.
     *
     * @param provider The provider responsible for fetching help entries.
     * @param rendererFactory The renderer factory to create layout renderers.
     *
     * @param <S> The type of the {@link Source}.
     * @return A new instance of {@code HelpCoordinator}.
     */
    public static <S extends Source> HelpCoordinator<S> create(
            HelpProvider<S> provider,
            HelpLayoutRendererFactory<S> rendererFactory
    ) {
        return new HelpCoordinator<>(provider, rendererFactory);
    }
    
    /**
     * A factory method to create a new HelpCoordinator with default settings.
     *
     * @param <S> The type of the {@link Source}.
     * @return A new instance of {@code HelpCoordinator} with default provider and renderer.
     */
    public static <S extends Source> HelpCoordinator<S> create() {
        return new HelpCoordinator<>(HelpProvider.defaultProvider(), HelpLayoutRendererFactory.defaultFactory());
    }
    
    /**
     * Displays the help documentation based on the given context and query.
     * <p>
     * This method fetches the relevant help entries, then uses the configured
     * renderer to display the information to the source.
     *
     * @param context The execution context of the command.
     * @param query The help query, specifying what help information is needed.
     * @param options Rendering options that control how the help is displayed.
     */
    public <C> void showHelp(
            ExecutionContext<S> context,
            HelpQuery<S> query,
            HelpRenderOptions<S, C> options
    ) {
        // Step 1: Get data
        HelpEntryList<S> entries = provider.provide(context.command(), query);
        
        // Step 2: Render the data
        this.render(context, entries, options);
    }
    
    private <C> void render(
            ExecutionContext<S> context,
            HelpEntryList<S> entries,
            HelpRenderOptions<S, C> options
    ) {
        HelpLayoutRenderer<S, C> renderer = rendererFactory.create(options);
        if (renderer == null) {
            throw new IllegalArgumentException("Unknown layout: " + options.getLayout());
        }
        
        HelpTheme<S, C> theme = options.getTheme();
        // Header
        if (theme.isOptionEnabled(HelpTheme.Option.SHOW_HEADER)) {
            theme.getHeader(context).send(context.source());
        }
        
        //rendering actual help
        renderer.render(context, entries, options);
        
        // Footer
        if (theme.isOptionEnabled(HelpTheme.Option.SHOW_FOOTER)) {
            theme.getFooter(context).send(context.source());
        }
    }
    
}
