package studio.mevera.imperat.command.tree.help.theme;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.context.Source;

public abstract class TextHelpTheme<S extends Source> extends BaseHelpTheme<S, String> {
    protected TextHelpTheme(
            @NotNull PresentationStyle style,
            int indentMultiplier
    ) {
        super(style, indentMultiplier, StringHelpComponent::new);
    }
}
