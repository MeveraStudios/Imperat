package studio.mevera.imperat.help;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.VelocitySource;
import studio.mevera.imperat.adventure.AdventureHelpComponent;
import java.util.function.BiConsumer;

/**
 * Velocity-specific implementation of Adventure-based help components.
 * This class provides rich text help messages for Velocity commands using the Adventure API,
 * allowing for styled, colored, and interactive help content.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Rich text formatting with Adventure Components</li>
 *   <li>Automatic integration with VelocitySource messaging</li>
 *   <li>Support for clickable, hoverable, and styled text</li>
 *   <li>Seamless help system integration</li>
 * </ul>
 *
 * <p>Usage Example:</p>
 * <pre>{@code
 * Component helpText = Component.text("Use /mycommand <player>")
 *     .color(NamedTextColor.YELLOW);
 * VelocityAdventureHelpComponent help = VelocityAdventureHelpComponent.of(helpText);
 * }</pre>
 *
 * @since 2.1.0
 * @author Imperat Framework
 * @see VelocitySource
 * @see AdventureHelpComponent
 */
public class VelocityAdventureHelpComponent extends AdventureHelpComponent<VelocitySource> {
    
    /**
     * Creates a new VelocityAdventureHelpComponent with the specified component and message sender.
     *
     * @param componentValue the Adventure Component to display as help
     * @param sendMessageToSourceFunc function to send the component to a VelocitySource
     */
    protected VelocityAdventureHelpComponent(
            @NotNull Component componentValue,
            BiConsumer<VelocitySource, Component> sendMessageToSourceFunc
    ) {
        super(componentValue, sendMessageToSourceFunc);
    }
    
    /**
     * Creates a new VelocityAdventureHelpComponent with the specified Adventure Component.
     * Uses the default VelocitySource reply method for sending messages.
     *
     * @param componentValue the Adventure Component to display as help
     * @return a new VelocityAdventureHelpComponent instance
     */
    public static VelocityAdventureHelpComponent of(
            @NotNull Component componentValue
    ) {
        return new VelocityAdventureHelpComponent(componentValue, VelocitySource::reply);
    }
}
