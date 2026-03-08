package studio.mevera.imperat.help;

import net.kyori.adventure.text.Component;
import studio.mevera.imperat.VelocityCommandSource;
import studio.mevera.imperat.command.tree.help.theme.BaseHelpTheme;

public abstract class AdventureHelpTheme extends BaseHelpTheme<VelocityCommandSource, Component> {

    protected AdventureHelpTheme() {
        super(VelocityAdventureHelpComponent::of);
    }
}
