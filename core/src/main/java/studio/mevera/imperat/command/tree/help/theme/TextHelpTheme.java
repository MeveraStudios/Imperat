package studio.mevera.imperat.command.tree.help.theme;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.context.CommandSource;

public abstract class TextHelpTheme<S extends CommandSource> extends BaseHelpTheme<S, String> {

    protected TextHelpTheme(
            @NotNull PresentationStyle style,
            int indentMultiplier
    ) {
        super(style, indentMultiplier, StringHelpComponent::new);
    }
}
