package studio.mevera.imperat.command.tree.help;

import studio.mevera.imperat.context.Source;

/**
 * Rendering options for customizing help display.
 * <p>
 * This class serves as a builder for configuring how help documentation should be rendered.
 * It encapsulates settings like the display theme and the layout style, providing a
 * fluent API for easy configuration.
 *
 * @param <S> The type of {@link Source} from which the command was executed.
 */
public class HelpRenderOptions<S extends Source> {
    
    private HelpTheme<S> theme = HelpTheme.defaultTheme();
    private Layout layout = Layout.LIST;
    
    /**
     * Private constructor to enforce the use of the static builder method.
     */
    private HelpRenderOptions() {
    
    }
    
    /**
     * Creates a new builder for configuring help rendering options.
     *
     * @param <S> The type of {@link Source}.
     * @return A new instance of {@code HelpRenderOptions}.
     */
    public static <S extends Source> HelpRenderOptions<S> builder() {
        return new HelpRenderOptions<S>();
    }
    
    /**
     * An enumeration of the available help documentation layouts.
     */
    public enum Layout {
        /** Simple list view where commands are displayed one after another. */
        LIST,
        
        /** A tree structure view that shows command nesting and hierarchy. */
        TREE,
    }
    
    /**
     * Gets the current rendering theme.
     *
     * @return The configured {@link HelpTheme}.
     */
    public HelpTheme<S> getTheme() {
        return theme;
    }
    
    /**
     * Gets the current rendering layout.
     *
     * @return The configured {@link Layout}.
     */
    public Layout getLayout() {
        return layout;
    }
    
    /**
     * Sets the rendering theme for the help output.
     *
     * @param theme The new help theme.
     * @return This builder instance for method chaining.
     */
    public HelpRenderOptions<S> theme(HelpTheme<S> theme) {
        this.theme = theme;
        return this;
    }
    
    /**
     * Sets the layout for the help output.
     *
     * @param layout The new help layout.
     * @return This builder instance for method chaining.
     */
    public HelpRenderOptions<S> layout(Layout layout) {
        this.layout = layout;
        return this;
    }
}
