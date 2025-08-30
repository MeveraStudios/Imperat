package studio.mevera.imperat.help;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.BukkitSource;
import studio.mevera.imperat.adventure.AdventureHelpComponent;
import java.util.function.BiConsumer;

public class BukkitAdventureHelpComponent extends AdventureHelpComponent<BukkitSource> {
    protected BukkitAdventureHelpComponent(
            @NotNull Component componentValue,
            BiConsumer<BukkitSource, Component> sendMessageToSourceFunc
    ) {
        super(componentValue, sendMessageToSourceFunc);
    }
    
    public static BukkitAdventureHelpComponent of(
            @NotNull Component componentValue
    ) {
        return new BukkitAdventureHelpComponent(componentValue, BukkitSource::reply);
    }
}
