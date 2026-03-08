package studio.mevera.imperat.help;

import net.kyori.adventure.text.Component;
import studio.mevera.imperat.BungeeCommandSource;
import studio.mevera.imperat.command.tree.help.theme.BaseHelpTheme;

public abstract class AdventureHelpTheme extends BaseHelpTheme<BungeeCommandSource, Component> {

    protected AdventureHelpTheme() {
        super(BungeeAdventureHelpComponent::of);
    }
}