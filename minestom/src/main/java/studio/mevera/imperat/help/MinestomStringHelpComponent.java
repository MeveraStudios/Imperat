package studio.mevera.imperat.help;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.MinestomSource;
import studio.mevera.imperat.command.tree.help.theme.StringHelpComponent;

/**
 * Minestom-specific implementation of string-based help components.
 * This class provides simple text-based help messages for Minestom commands,
 * with support for basic text formatting.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Simple string-based help messages</li>
 *   <li>Automatic integration with MinestomSource messaging</li>
 *   <li>Modern Minecraft server help display</li>
 *   <li>Lightweight alternative to Adventure Component-based help</li>
 * </ul>
 *
 * <p>Usage Example:</p>
 * <pre>{@code
 * MinestomStringHelpComponent help = MinestomStringHelpComponent.of(
 *     "Use /tp <player> - Teleport to a player"
 * );
 * }</pre>
 *
 * @since 2.1.0
 * @author Imperat Framework
 * @see MinestomSource
 * @see StringHelpComponent
 */
public class MinestomStringHelpComponent extends StringHelpComponent<MinestomSource> {
    
    /**
     * Creates a new MinestomStringHelpComponent with the specified string content.
     *
     * @param componentValue the string content to display as help
     */
    protected MinestomStringHelpComponent(@NotNull String componentValue) {
        super(componentValue);
    }
    
    /**
     * Creates a new MinestomStringHelpComponent with the specified string content.
     *
     * @param componentValue the string content to display as help
     * @return a new MinestomStringHelpComponent instance
     */
    public static MinestomStringHelpComponent of(@NotNull String componentValue) {
        return new MinestomStringHelpComponent(componentValue);
    }
}
