package studio.mevera.imperat.help;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.MinestomSource;
import studio.mevera.imperat.adventure.AdventureHelpComponent;
import java.util.function.BiConsumer;

/**
 * Minestom-specific implementation of Adventure-based help components.
 * This class provides rich text help messages for Minestom commands using the Adventure API,
 * leveraging Minestom's native Adventure integration for optimal performance and features.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Rich text formatting with Adventure Components</li>
 *   <li>Native Minestom Adventure API integration</li>
 *   <li>Support for clickable, hoverable, and styled text</li>
 *   <li>High-performance component messaging</li>
 *   <li>Modern Minecraft server help system</li>
 * </ul>
 *
 * <p>Usage Example:</p>
 * <pre>{@code
 * Component helpText = Component.text("Use /tp <player>")
 *     .color(NamedTextColor.AQUA)
 *     .hoverEvent(HoverEvent.showText(Component.text("Teleport to player")));
 * MinestomAdventureHelpComponent help = MinestomAdventureHelpComponent.of(helpText);
 * }</pre>
 *
 * @since 2.1.0
 * @author Imperat Framework
 * @see MinestomSource
 * @see AdventureHelpComponent
 */
public class MinestomAdventureHelpComponent extends AdventureHelpComponent<MinestomSource> {
    
    /**
     * Creates a new MinestomAdventureHelpComponent with the specified component and message sender.
     *
     * @param componentValue the Adventure Component to display as help
     * @param sendMessageToSourceFunc function to send the component to a MinestomSource
     */
    protected MinestomAdventureHelpComponent(
            @NotNull Component componentValue,
            BiConsumer<MinestomSource, Component> sendMessageToSourceFunc
    ) {
        super(componentValue, sendMessageToSourceFunc);
    }
    
    /**
     * Creates a new MinestomAdventureHelpComponent with the specified Adventure Component.
     * Uses the default MinestomSource sendMessage method for sending messages.
     *
     * @param componentValue the Adventure Component to display as help
     * @return a new MinestomAdventureHelpComponent instance
     */
    public static MinestomAdventureHelpComponent of(
            @NotNull Component componentValue
    ) {
        return new MinestomAdventureHelpComponent(componentValue, MinestomSource::reply);
    }
}
