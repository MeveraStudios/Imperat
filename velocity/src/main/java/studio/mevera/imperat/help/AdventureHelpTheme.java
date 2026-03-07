package studio.mevera.imperat.help;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.VelocityCommandSource;
import studio.mevera.imperat.command.tree.help.theme.BaseHelpTheme;

public abstract class AdventureHelpTheme extends BaseHelpTheme<VelocityCommandSource, Component> {

    protected AdventureHelpTheme(
            @NotNull PresentationStyle style,
            int indentMultiplier
    ) {
        super(style, indentMultiplier, VelocityAdventureHelpComponent::of);
    }
}
