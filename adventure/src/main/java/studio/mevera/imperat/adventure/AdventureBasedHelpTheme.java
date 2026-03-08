package studio.mevera.imperat.adventure;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.tree.help.theme.BaseHelpTheme;

public abstract class AdventureBasedHelpTheme<S extends AdventureCommandSource> extends BaseHelpTheme<S, Component> {

    public AdventureBasedHelpTheme() {
        super((comp) -> new AdventureHelpComponent<>(comp, AdventureCommandSource::reply));
    }

    @Override
    public final @NotNull Component createEmptyContent() {
        return Component.empty();
    }


}
