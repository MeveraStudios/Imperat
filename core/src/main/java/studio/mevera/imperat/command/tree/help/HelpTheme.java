package studio.mevera.imperat.command.tree.help;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.tree.help.renderers.UsageFormatter;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;

/**
 * A theme for help documentation generation and presentation.
 * @param <S> the type of command source/console
 * @param <C> the type of the entries in the help component (can be String, Component, etc.)
 */
public interface HelpTheme<S extends Source, C> {
    
    /**
     * Available presentation styles for help display.
     */
    enum PresentationStyle {
        /** Tree structure view that shows command nesting and hierarchy. */
        TREE,
        
        /** Simple flat list where commands are displayed one after another. */
        FLAT
    }
    
    HelpComponent<S, C> createEmptyComponent();
    
    /**
     * Gets the preferred presentation style for this theme.
     *
     * @return the presentation style to use
     */
    @NotNull PresentationStyle getPreferredStyle();
    
    // FOR TREE LAYOUTS
    
    int getIndentMultiplier();
    
    @NotNull HelpComponent<S, C> getBranch();
    @NotNull HelpComponent<S, C> getLastBranch();
    @NotNull HelpComponent<S, C> getIndent();
    @NotNull HelpComponent<S, C> getEmptyIndent();
    default @NotNull HelpComponent<S, C> getTreeBranch(boolean isLast) {
        return isLast ? getLastBranch() : getBranch();
    }
    
    default @NotNull HelpComponent<S, C> getTreeIndent(boolean hasMore) {
        HelpComponent<S, C> base = hasMore ? getIndent() : getEmptyIndent();
        if (getIndentMultiplier() > 1) {
            return base.repeat(getIndentMultiplier());
        }
        return base;
    }
    
    //GENERAL HELP PARTS
    @NotNull HelpComponent<S, C> getHeader(ExecutionContext<S> context);
    
    @NotNull HelpComponent<S, C> getFooter(ExecutionContext<S> context);
    
    boolean isOptionEnabled(@NotNull Option option);
    
    enum Option {
        
        SHOW_HEADER,
        
        SHOW_FOOTER;
    }
    
    @NotNull UsageFormatter<S, C> getUsageFormatter();
}