package studio.mevera.imperat.command.tree.help.theme;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.tree.help.renderers.UsageFormatter;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.ExecutionContext;

/**
 * A theme for help documentation generation and presentation.
 * @param <S> the type of command source/console
 * @param <C> the type of the entries in the help component (can be String, Component, etc.)
 */
public interface HelpTheme<S extends CommandSource, C> {

    HelpComponent<S, C> createEmptyComponent();

    <T> void setOptionValue(@NotNull Option<T> option, T value);

    //GENERAL HELP PARTS
    @NotNull HelpComponent<S, C> getHeader(ExecutionContext<S> context);

    @NotNull HelpComponent<S, C> getFooter(ExecutionContext<S> context);

    <T> T getOptionValue(@NotNull Option<T> option);

    @NotNull UsageFormatter<S, C> getUsageFormatter();


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