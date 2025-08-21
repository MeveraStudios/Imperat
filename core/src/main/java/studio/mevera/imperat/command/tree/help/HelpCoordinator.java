package studio.mevera.imperat.command.tree.help;

import java.util.function.UnaryOperator;
import studio.mevera.imperat.command.tree.help.renderers.layouts.HelpLayoutRendererManager;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;


/**
 * Main help coordinator that ties everything together.
 * <p>
 * This class orchestrates the entire help system, from fetching the relevant
 * help entries to transforming and rendering them to the user. It is configured
 * with a {@link HelpProvider} to get the data and a {@link HelpLayoutRendererManager}
 * to handle the rendering logic.
 *
 * @param <S> The type of the {@link Source} from which the command was executed.
 */
public class HelpCoordinator<S extends Source> {
    
    private final HelpProvider<S> provider;
    private final HelpLayoutRendererManager<S> renderer;
    
    /**
     * Constructs a new HelpCoordinator with a custom provider and renderer manager.
     *
     * @param provider The provider responsible for fetching help entries.
     * @param rendererManagerModifier A modifier to customize the default renderer manager.
     */
    private HelpCoordinator(
            HelpProvider<S> provider,
            UnaryOperator<HelpLayoutRendererManager<S>> rendererManagerModifier
    ) {
        this.provider = provider;
        this.renderer = rendererManagerModifier.apply(new HelpLayoutRendererManager<>());
    }
    
    /**
     * Constructs a new HelpCoordinator with default settings.
     */
    private HelpCoordinator() {
        this(HelpProvider.defaultProvider(), UnaryOperator.identity());
    }
    
    /**
     * A factory method to create a new HelpCoordinator with custom settings.
     *
     * @param provider The provider responsible for fetching help entries.
     * @param rendererManagerModifier A modifier to customize the default renderer manager.
     * @param <S> The type of the {@link Source}.
     * @return A new instance of {@code HelpCoordinator}.
     */
    public static <S extends Source> HelpCoordinator<S> create(
            HelpProvider<S> provider,
            UnaryOperator<HelpLayoutRendererManager<S>> rendererManagerModifier
    ) {
        return new HelpCoordinator<>(provider, rendererManagerModifier);
    }
    
    /**
     * A factory method to create a new HelpCoordinator with default settings.
     *
     * @param <S> The type of the {@link Source}.
     * @return A new instance of {@code HelpCoordinator} with default provider and renderer.
     */
    public static <S extends Source> HelpCoordinator<S> create(UnaryOperator<HelpLayoutRendererManager<S>> rendererManagerModifier) {
        return new HelpCoordinator<>(HelpProvider.defaultProvider(), rendererManagerModifier);
    }
    
    /**
     * A factory method to create a new HelpCoordinator with default settings.
     *
     * @param <S> The type of the {@link Source}.
     * @return A new instance of {@code HelpCoordinator} with default provider and renderer.
     */
    public static <S extends Source> HelpCoordinator<S> create() {
        return new HelpCoordinator<>();
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
    public void showHelp(
            ExecutionContext<S> context,
            HelpQuery<S> query,
            HelpRenderOptions<S> options
    ) {
        // Step 1: Get data
        HelpEntryList<S> entries = provider.provide(context.command(), query);
        
        // Step 2: Render the data
        this.renderer.render(context, entries, options);
    }
}
