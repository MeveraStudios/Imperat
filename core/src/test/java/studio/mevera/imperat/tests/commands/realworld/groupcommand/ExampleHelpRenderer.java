package studio.mevera.imperat.tests.commands.realworld.groupcommand;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.tree.help.HelpEntry;
import studio.mevera.imperat.command.tree.help.HelpResult;
import studio.mevera.imperat.command.tree.help.renderers.HelpRenderer;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.tests.TestCommandSource;

import java.util.ArrayList;
import java.util.List;

public class ExampleHelpRenderer implements HelpRenderer<TestCommandSource, String> {

    @Override
    public @NotNull List<String> render(
            @NotNull ExecutionContext<TestCommandSource> context,
            @NotNull HelpResult<TestCommandSource> help
    ) {
        List<String> lines = new ArrayList<>(help.size() + 2);
        lines.add("======== Command Help ========");
        for (HelpEntry<TestCommandSource> entry : help) {
            lines.add(context.command().getName() + " " + entry.getUsage());
        }
        lines.add("=============================");
        return List.copyOf(lines);
    }
}
