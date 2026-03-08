package studio.mevera.imperat.help;

import net.kyori.adventure.text.Component;
import studio.mevera.imperat.BukkitCommandSource;
import studio.mevera.imperat.command.tree.help.theme.BaseHelpTheme;

public abstract class AdventureHelpTheme extends BaseHelpTheme<BukkitCommandSource, Component> {

    protected AdventureHelpTheme() {
        super(BukkitAdventureHelpComponent::of);
    }
}
