package studio.mevera.imperat.command.tree.help;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.annotations.types.Context;
import studio.mevera.imperat.command.tree.help.theme.HelpTheme;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;

@Context
public final class CommandHelp<S extends Source> {

    private final @NotNull ExecutionContext<S> context;
    private final @NotNull HelpCoordinator<S> coordinator;

    private CommandHelp(@NotNull ExecutionContext<S> context) {
        this.context = context;
        this.coordinator = context.imperatConfig().getHelpCoordinator();
    }

    public static <S extends Source> CommandHelp<S> create(ExecutionContext<S> context) {
        return new CommandHelp<>(context);
    }

    /**
     * Displays help using the provided query and theme.
     *
     * @param query the help query specifying what help to show
     * @param theme the theme controlling how help is displayed
     */
    public <C> void display(
            HelpQuery<S> query,
            HelpTheme<S, C> theme
    ) {
        coordinator.showHelp(context, query, theme);
    }

    public @NotNull ExecutionContext<S> getContext() {
        return context;
    }

    public @NotNull HelpCoordinator<S> getCoordinator() {
        return coordinator;
    }
}