package studio.mevera.imperat.command.tree.help;

import studio.mevera.imperat.command.tree.help.renderers.HelpLayoutRenderer;
import studio.mevera.imperat.command.tree.help.renderers.StandardHelpRenderer;
import studio.mevera.imperat.command.tree.help.theme.HelpTheme;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;


public class HelpCoordinator<S extends Source> {

    private final TreeHelpVisitor<S> provider;
    private final HelpLayoutRenderer<S, ?> renderer;

    /**
     * Constructs a new HelpCoordinator with a custom provider and renderer.
     *
     * @param provider The provider responsible for fetching help entries.
     * @param renderer The renderer responsible for displaying help entries.
     */
    private HelpCoordinator(
            TreeHelpVisitor<S> provider,
            HelpLayoutRenderer<S, ?> renderer
    ) {
        this.provider = provider;
        this.renderer = renderer;
    }

    /**
     * A factory method to create a new HelpCoordinator with custom settings.
     *
     * @param provider The provider responsible for fetching help entries.
     * @param renderer The renderer responsible for displaying help entries.
     *
     * @param <S> The type of the {@link Source}.
     * @return A new instance of {@code HelpCoordinator}.
     */
    public static <S extends Source> HelpCoordinator<S> create(
            TreeHelpVisitor<S> provider,
            HelpLayoutRenderer<S, ?> renderer
    ) {
        return new HelpCoordinator<>(provider, renderer);
    }

    /**
     * A factory method to create a new HelpCoordinator with default settings.
     *
     * @param <S> The type of the {@link Source}.
     * @return A new instance of {@code HelpCoordinator} with default provider and renderer.
     */
    public static <S extends Source> HelpCoordinator<S> create() {
        return new HelpCoordinator<>(TreeHelpVisitor.defaultProvider(), new StandardHelpRenderer<>());
    }

    /**
     * Displays the help documentation based on the given context and query.
     * <p>
     * This method fetches the relevant help entries, then uses the configured
     * renderer to display the information to the source.
     *
     * @param context The execution context of the command.
     * @param query The help query, specifying what help information is needed.
     * @param theme The theme that controls how the help is displayed.
     */
    @SuppressWarnings("unchecked")
    public <C> void showHelp(
            ExecutionContext<S> context,
            HelpQuery<S> query,
            HelpTheme<S, C> theme
    ) {
        // Step 1: Get data
        HelpEntryList<S> entries = provider.visit(context.command(), query);

        // Step 2: Render the data
        this.render(context, entries, theme);
    }

    @SuppressWarnings("unchecked")
    private <C> void render(
            ExecutionContext<S> context,
            HelpEntryList<S> entries,
            HelpTheme<S, C> theme
    ) {
        HelpLayoutRenderer<S, C> typedRenderer = (HelpLayoutRenderer<S, C>) renderer;

        // Header
        if (theme.getOptionValue(HelpTheme.Option.SHOW_HEADER)) {
            theme.getHeader(context).send(context.source());
        }

        // Rendering actual help
        typedRenderer.render(context, entries, theme);

        // Footer
        if (theme.getOptionValue(HelpTheme.Option.SHOW_FOOTER)) {
            theme.getFooter(context).send(context.source());
        }
    }
}