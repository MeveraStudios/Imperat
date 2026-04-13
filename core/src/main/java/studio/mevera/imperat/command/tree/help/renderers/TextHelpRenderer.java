package studio.mevera.imperat.command.tree.help.renderers;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.tree.help.HelpEntry;
import studio.mevera.imperat.command.tree.help.HelpResult;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.ExecutionContext;

import java.util.ArrayList;
import java.util.List;

public final class TextHelpRenderer<S extends CommandSource> implements HelpRenderer<S, String> {

    private final boolean showHeader;
    private final boolean showFooter;
    private final boolean includeDescriptions;

    private TextHelpRenderer(boolean showHeader, boolean showFooter, boolean includeDescriptions) {
        this.showHeader = showHeader;
        this.showFooter = showFooter;
        this.includeDescriptions = includeDescriptions;
    }

    public static <S extends CommandSource> @NotNull TextHelpRenderer<S> create() {
        return new TextHelpRenderer<>(true, true, true);
    }

    @Override
    public @NotNull List<String> render(
            @NotNull ExecutionContext<S> context,
            @NotNull HelpResult<S> help
    ) {
        List<String> lines = new ArrayList<>(help.size() + 2);
        String label = context.imperatConfig().commandPrefix() + context.getRootCommandLabelUsed();

        if (showHeader) {
            lines.add("Help for " + label);
        }

        for (HelpEntry<S> entry : help) {
            String line = label;
            if (!entry.getUsage().isBlank()) {
                line += " " + entry.getUsage();
            }

            if (includeDescriptions && entry.getDescription() != null && !entry.getDescription().isBlank()) {
                line += " - " + entry.getDescription();
            }
            lines.add(line);
        }

        if (showFooter) {
            lines.add("Displayed " + help.size() + " help entr" + (help.size() == 1 ? "y" : "ies"));
        }

        return List.copyOf(lines);
    }
}
