package studio.mevera.imperat.help;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.VelocitySource;
import studio.mevera.imperat.command.tree.help.theme.StringHelpComponent;

/**
 * Velocity-specific implementation of string-based help components.
 * This class provides simple text-based help messages for Velocity commands,
 * supporting MiniMessage formatting for basic styling and colors.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Simple string-based help messages</li>
 *   <li>MiniMessage format support for colors and basic styling</li>
 *   <li>Automatic integration with VelocitySource messaging</li>
 *   <li>Lightweight alternative to Adventure Component-based help</li>
 * </ul>
 *
 * <p>Usage Example:</p>
 * <pre>{@code
 * VelocityStringHelpComponent help = VelocityStringHelpComponent.of(
 *     "<yellow>Use /mycommand <player> - Teleport to a player</yellow>"
 * );
 * }</pre>
 *
 * @since 2.1.0
 * @author Imperat Framework
 * @see VelocitySource
 * @see StringHelpComponent
 */
public class VelocityStringHelpComponent extends StringHelpComponent<VelocitySource> {

    /**
     * Creates a new VelocityStringHelpComponent with the specified string content.
     *
     * @param componentValue the string content to display as help (supports MiniMessage formatting)
     */
    protected VelocityStringHelpComponent(@NotNull String componentValue) {
        super(componentValue);
    }

    /**
     * Creates a new VelocityStringHelpComponent with the specified string content.
     *
     * @param componentValue the string content to display as help (supports MiniMessage formatting)
     * @return a new VelocityStringHelpComponent instance
     */
    public static VelocityStringHelpComponent of(@NotNull String componentValue) {
        return new VelocityStringHelpComponent(componentValue);
    }
}
