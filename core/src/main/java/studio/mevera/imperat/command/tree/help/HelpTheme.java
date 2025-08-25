package studio.mevera.imperat.command.tree.help;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.tree.help.renderers.layouts.UsageDecorator;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;

/**
 * A theme for help documentation generation and presentation.
 * @param <S> the type of command source/console
 * @param <C> the type of the entries in the help component (can be String, Component, etc.)
 */
public interface HelpTheme<S extends Source, C> {
    
    // FOR TREE LAYOUTS
    
    int getIndentMultiplier();
    
    @NotNull HelpComponent<C> getBranch();
    @NotNull HelpComponent<C> getLastBranch();
    @NotNull HelpComponent<C> getIndent();
    @NotNull HelpComponent<C> getEmptyIndent();
    default @NotNull HelpComponent<C> getTreeBranch(boolean isLast) {
        return isLast ? getLastBranch() : getBranch();
    }
    
    @SuppressWarnings("unchecked")
    default @NotNull HelpComponent<C> getTreeIndent(boolean hasMore) {
        HelpComponent<C> base = hasMore ? getIndent() : getEmptyIndent();
        if (getIndentMultiplier() > 1) {
            return (HelpComponent<C>) base.repeat(getIndentMultiplier());
        }
        return base;
    }
    
    //FOR LIST FORMATTING
    @NotNull HelpComponent<C> getPathSeparator();
    
    //GENERAL HELP PARTS
    @NotNull HelpComponent<C> getHeader(ExecutionContext<S> context);
    
    @NotNull HelpComponent<C> getFooter(ExecutionContext<S> context);
    
    boolean isOptionEnabled(@NotNull Option option);
    
    enum Option {
        
        SHOW_HEADER,
        
        SHOW_FOOTER;
    }
    
    @NotNull UsageDecorator<S, C> getUsageDecorator();
}
