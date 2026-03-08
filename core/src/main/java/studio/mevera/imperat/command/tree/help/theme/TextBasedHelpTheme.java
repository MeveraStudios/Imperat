package studio.mevera.imperat.command.tree.help.theme;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.context.CommandSource;

public abstract class TextBasedHelpTheme<S extends CommandSource> extends BaseHelpTheme<S, String> {

    public TextBasedHelpTheme() {
        super(HelpComponent::plainText);
    }

    @Override
    public final @NotNull String createEmptyContent() {
        return "";
    }
}
