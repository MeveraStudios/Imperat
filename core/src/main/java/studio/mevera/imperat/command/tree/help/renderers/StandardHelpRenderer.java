package studio.mevera.imperat.command.tree.help.renderers;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.tree.help.HelpEntry;
import studio.mevera.imperat.command.tree.help.HelpEntryList;
import studio.mevera.imperat.command.tree.help.theme.HelpComponent;
import studio.mevera.imperat.command.tree.help.theme.HelpTheme;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.ExecutionContext;

/**
 * Standard help renderer that displays help entries in a flat list format.
 * Each entry is formatted using the theme's usage formatter and sent to the source.
 *
 * @param <S> the source type
 * @param <C> the component type
 */
public final class StandardHelpRenderer<S extends CommandSource, C> implements HelpLayoutRenderer<S, C> {

    @Override
    public void render(
            @NotNull ExecutionContext<S> context,
            @NotNull HelpEntryList<S> helpEntries,
            @NotNull HelpTheme<S, C> theme
    ) {
        S source = context.source();

        for (HelpEntry<S> entry : helpEntries) {
            HelpComponent<S, C> component = theme.getUsageFormatter().format(
                    context.command(),
                    entry.getPathway(),
                    context,
                    theme
            );

            component.send(source);
        }
    }

}