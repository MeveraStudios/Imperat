package studio.mevera.imperat.command.tree.help.theme;

import studio.mevera.imperat.context.CommandSource;

public abstract class TextHelpTheme<S extends CommandSource> extends BaseHelpTheme<S, String> {

    protected TextHelpTheme() {
        super(StringHelpComponent::new);
    }
}
