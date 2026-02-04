package studio.mevera.imperat.command.tree.help.theme;

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

    HelpComponent<S, C> createEmptyComponent();

    /**
     * Gets the preferred presentation style for this theme.
     *
     * @return the presentation style to use
     */
    @NotNull PresentationStyle getPreferredStyle();

    int getIndentMultiplier();

    // FOR TREE LAYOUTS

    @NotNull HelpComponent<S, C> getBranch();

    @NotNull HelpComponent<S, C> getLastBranch();

    @NotNull HelpComponent<S, C> getIndent();

    @NotNull HelpComponent<S, C> getEmptyIndent();

    <T> void setOptionValue(@NotNull Option<T> option, T value);

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

    <T> T getOptionValue(@NotNull Option<T> option);

    @NotNull UsageFormatter<S, C> getUsageFormatter();

    /**
     * Available presentation styles for help display.
     */
    enum PresentationStyle {
        /** Tree structure view that shows command nesting and hierarchy. */
        TREE,

        /** Simple flat list where commands are displayed one after another. */
        FLAT
    }

    interface Option<T> {

        Option<Boolean> SHOW_HEADER = new Option<>() {
            @Override
            public @NotNull String id() {
                return "show_header";
            }

            @Override
            public @NotNull Boolean defaultValue() {
                return false;
            }
        };
        Option<Boolean> SHOW_FOOTER = new Option<>() {
            @Override
            public @NotNull String id() {
                return "show_footer";
            }

            @Override
            public @NotNull Boolean defaultValue() {
                return false;
            }
        };

        //to string constants
        @NotNull
        String id();

        @NotNull T defaultValue();

    }
}