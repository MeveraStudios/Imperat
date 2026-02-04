package studio.mevera.imperat.command.tree.help.renderers;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.command.tree.help.theme.HelpComponent;
import studio.mevera.imperat.command.tree.help.theme.HelpTheme;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;

/**
 * Interface for formatting command usage paths into displayable help components.
 * Renamed from UsageDecorator to better reflect its responsibility as a formatter.
 *
 * @param <S> the source type
 * @param <C> the component type
 */
public interface UsageFormatter<S extends Source, C> {

    /**
     * Formats a command usage pathway into a help component.
     *
     * @param lastOwningCommand the command that owns this usage
     * @param pathway the usage pathway to format
     * @param context the execution context
     * @param theme the help theme
     * @return a formatted help component
     */
    @NotNull HelpComponent<S, C> format(
            Command<S> lastOwningCommand,
            CommandUsage<S> pathway,
            ExecutionContext<S> context,
            HelpTheme<S, C> theme
    );
}