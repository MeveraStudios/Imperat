package studio.mevera.imperat.help;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.BungeeSource;
import studio.mevera.imperat.command.tree.help.StringHelpComponent;

/**
 * BungeeCord-specific implementation of string-based help components.
 * This class provides simple text-based help messages for BungeeCord commands,
 * supporting legacy formatting and basic color codes.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Simple string-based help messages</li>
 *   <li>Legacy color code support for backward compatibility</li>
 *   <li>Automatic integration with BungeeSource messaging</li>
 *   <li>Cross-server command help display</li>
 * </ul>
 *
 * <p>Usage Example:</p>
 * <pre>{@code
 * BungeeStringHelpComponent help = BungeeStringHelpComponent.of(
 *     "&eUse /server <name> - Connect to a server"
 * );
 * }</pre>
 *
 * @since 2.1.0
 * @author Imperat Framework
 * @see BungeeSource
 * @see StringHelpComponent
 */
public class BungeeStringHelpComponent extends StringHelpComponent<BungeeSource> {
    
    /**
     * Creates a new BungeeStringHelpComponent with the specified string content.
     *
     * @param componentValue the string content to display as help (supports legacy color codes)
     */
    protected BungeeStringHelpComponent(@NotNull String componentValue) {
        super(componentValue);
    }
    
    /**
     * Creates a new BungeeStringHelpComponent with the specified string content.
     *
     * @param componentValue the string content to display as help (supports legacy color codes)
     * @return a new BungeeStringHelpComponent instance
     */
    public static BungeeStringHelpComponent of(@NotNull String componentValue) {
        return new BungeeStringHelpComponent(componentValue);
    }
}
