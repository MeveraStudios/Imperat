package studio.mevera.imperat.adventure;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.tree.help.HelpEntry;
import studio.mevera.imperat.command.tree.help.HelpResult;
import studio.mevera.imperat.command.tree.help.renderers.HelpRenderer;
import studio.mevera.imperat.context.ExecutionContext;

import java.util.ArrayList;
import java.util.List;

public final class AdventureHelpRenderer<S extends AdventureCommandSource> implements HelpRenderer<S, Component> {

    private final boolean showHeader;
    private final boolean showFooter;
    private final boolean includeDescriptions;

    private AdventureHelpRenderer(boolean showHeader, boolean showFooter, boolean includeDescriptions) {
        this.showHeader = showHeader;
        this.showFooter = showFooter;
        this.includeDescriptions = includeDescriptions;
    }

    public static <S extends AdventureCommandSource> @NotNull AdventureHelpRenderer<S> create() {
        return new AdventureHelpRenderer<>(true, true, true);
    }

    @Override
    public @NotNull List<Component> render(
            @NotNull ExecutionContext<S> context,
            @NotNull HelpResult<S> help
    ) {
        List<Component> lines = new ArrayList<>(help.size() + 2);
        String label = context.imperatConfig().commandPrefix() + context.getRootCommandLabelUsed();

        if (showHeader) {
            lines.add(Component.text("Help for " + label));
        }

        for (HelpEntry<S> entry : help) {
            String line = label;
            if (!entry.getUsage().isBlank()) {
                line += " " + entry.getUsage();
            }
            if (includeDescriptions && entry.getDescription() != null && !entry.getDescription().isBlank()) {
                line += " - " + entry.getDescription();
            }
            lines.add(Component.text(line));
        }

        if (showFooter) {
            lines.add(Component.text("Displayed " + help.size() + " help entr" + (help.size() == 1 ? "y" : "ies")));
        }

        return List.copyOf(lines);
    }
}
