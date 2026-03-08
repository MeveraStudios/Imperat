package studio.mevera.imperat.tests.commands.realworld.groupcommand;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.tree.help.renderers.UsageFormatter;
import studio.mevera.imperat.command.tree.help.theme.HelpComponent;
import studio.mevera.imperat.command.tree.help.theme.TextBasedHelpTheme;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.tests.TestCommandSource;

public class ExampleHelpTheme extends TextBasedHelpTheme<TestCommandSource> {

    public ExampleHelpTheme() {
        super();
    }


    @Override
    public @NotNull String getHeaderContent(ExecutionContext<TestCommandSource> context) {
        return "======== Command Help ========";
    }

    @Override
    public @NotNull String getFooterContent(ExecutionContext<TestCommandSource> context) {
        return "=============================";
    }

    @Override
    public @NotNull UsageFormatter<TestCommandSource, String> getUsageFormatter() {
        return (
                lastCommand,
                pathway,
                context,
                theme
        ) -> {
            return HelpComponent.plainText(
                    context.command().getName() + " " + pathway.formatted()
            );
        };
    }

}
