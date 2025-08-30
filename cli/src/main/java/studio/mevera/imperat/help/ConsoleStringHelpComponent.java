package studio.mevera.imperat.help;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.ConsoleSource;
import studio.mevera.imperat.command.tree.help.theme.StringHelpComponent;

/**
 * Command-line interface implementation of string-based help components.
 * This class provides simple text-based help messages for CLI commands,
 * designed for console applications and command-line tools.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Simple string-based help messages</li>
 *   <li>Automatic integration with ConsoleSource messaging</li>
 *   <li>Console-friendly formatting</li>
 *   <li>Lightweight design for CLI applications</li>
 *   <li>No external dependencies</li>
 * </ul>
 *
 * <p>Usage Example:</p>
 * <pre>{@code
 * ConsoleStringHelpComponent help = ConsoleStringHelpComponent.of(
 *     "Use: myapp process <file> - Process the specified file"
 * );
 * }</pre>
 *
 * @since 2.1.0
 * @author Imperat Framework
 * @see ConsoleSource
 * @see StringHelpComponent
 */
public class ConsoleStringHelpComponent extends StringHelpComponent<ConsoleSource> {
    
    /**
     * Creates a new ConsoleStringHelpComponent with the specified string content.
     *
     * @param componentValue the string content to display as help
     */
    protected ConsoleStringHelpComponent(@NotNull String componentValue) {
        super(componentValue);
    }
    
    /**
     * Creates a new ConsoleStringHelpComponent with the specified string content.
     *
     * @param componentValue the string content to display as help
     * @return a new ConsoleStringHelpComponent instance
     */
    public static ConsoleStringHelpComponent of(@NotNull String componentValue) {
        return new ConsoleStringHelpComponent(componentValue);
    }
}
