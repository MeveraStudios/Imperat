package studio.mevera.imperat.help;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.BungeeSource;
import studio.mevera.imperat.adventure.AdventureHelpComponent;

import java.util.function.BiConsumer;

/**
 * BungeeCord-specific implementation of Adventure-based help components.
 * This class provides rich text help messages for BungeeCord commands using the Adventure API,
 * allowing for styled, colored, and interactive help content in proxy environments.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Rich text formatting with Adventure Components</li>
 *   <li>Automatic integration with BungeeSource messaging</li>
 *   <li>Support for clickable, hoverable, and styled text</li>
 *   <li>Cross-server help system integration</li>
 *   <li>Modern messaging for BungeeCord networks</li>
 * </ul>
 *
 * <p>Usage Example:</p>
 * <pre>{@code
 * Component helpText = Component.text("Use /server <name>")
 *     .color(NamedTextColor.GOLD)
 *     .clickEvent(ClickEvent.suggestCommand("/server "));
 * BungeeAdventureHelpComponent help = BungeeAdventureHelpComponent.of(helpText);
 * }</pre>
 *
 * @author Imperat Framework
 * @see BungeeSource
 * @see AdventureHelpComponent
 * @since 2.1.0
 */
public class BungeeAdventureHelpComponent extends AdventureHelpComponent<BungeeSource> {

    /**
     * Creates a new BungeeAdventureHelpComponent with the specified component and message sender.
     *
     * @param componentValue          the Adventure Component to display as help
     * @param sendMessageToSourceFunc function to send the component to a BungeeSource
     */
    protected BungeeAdventureHelpComponent(
            @NotNull Component componentValue,
            BiConsumer<BungeeSource, Component> sendMessageToSourceFunc
    ) {
        super(componentValue, sendMessageToSourceFunc);
    }

    /**
     * Creates a new BungeeAdventureHelpComponent with the specified Adventure Component.
     * Uses the default BungeeSource reply method for sending messages.
     *
     * @param componentValue the Adventure Component to display as help
     * @return a new BungeeAdventureHelpComponent instance
     */
    public static BungeeAdventureHelpComponent of(
            @NotNull Component componentValue
    ) {
        return new BungeeAdventureHelpComponent(componentValue, BungeeSource::reply);
    }
}
