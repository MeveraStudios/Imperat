package studio.mevera.imperat.help;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.BukkitSource;
import studio.mevera.imperat.command.tree.help.theme.BaseHelpTheme;

public abstract class AdventureHelpTheme extends BaseHelpTheme<BukkitSource, Component> {
    protected AdventureHelpTheme(
            @NotNull PresentationStyle style,
            int indentMultiplier
    ) {
        super(style, indentMultiplier, BukkitAdventureHelpComponent::of);
    }
}
