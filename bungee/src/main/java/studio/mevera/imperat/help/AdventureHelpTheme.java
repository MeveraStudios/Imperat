package studio.mevera.imperat.help;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.BungeeSource;
import studio.mevera.imperat.command.tree.help.theme.BaseHelpTheme;
import studio.mevera.imperat.command.tree.help.theme.HelpTheme;

public abstract class AdventureHelpTheme extends BaseHelpTheme<BungeeSource, Component> {
    protected AdventureHelpTheme(
            @NotNull HelpTheme.PresentationStyle style,
            int indentMultiplier
    ) {
        super(style, indentMultiplier, BungeeAdventureHelpComponent::of);
    }
}