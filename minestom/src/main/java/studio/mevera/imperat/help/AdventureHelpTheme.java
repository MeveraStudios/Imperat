package studio.mevera.imperat.help;

import net.kyori.adventure.text.Component;
import studio.mevera.imperat.MinestomCommandSource;
import studio.mevera.imperat.command.tree.help.theme.BaseHelpTheme;

public abstract class AdventureHelpTheme extends BaseHelpTheme<MinestomCommandSource, Component> {

    protected AdventureHelpTheme() {
        super(MinestomAdventureHelpComponent::of);
    }
}
