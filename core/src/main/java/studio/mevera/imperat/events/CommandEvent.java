package studio.mevera.imperat.events;

import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.context.Source;

/**
 * Base class for all command-related events in the Imperat framework.
 *
 * <p>This abstract class provides a common foundation for events that are
 * associated with a specific {@link Command} instance. All command lifecycle
 * events (registration, execution, etc.) extend this class.</p>
 *
 * <p>Events extending this class are guaranteed to have access to the command
 * that triggered the event, enabling handlers to inspect command metadata,
 * permissions, usage patterns, and other command-specific information.</p>
 *
 * <h2>Common Subclasses</h2>
 * <ul>
 *     <li>{@link studio.mevera.imperat.events.types.CommandPreRegistrationEvent} - Before command registration</li>
 *     <li>{@link studio.mevera.imperat.events.types.CommandPostRegistrationEvent} - After command registration</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a custom command event
 * public class CommandExecutionEvent<S extends Source> extends CommandEvent<S> {
 *     private final ExecutionContext<S> context;
 *
 *     public CommandExecutionEvent(Command<S> command, ExecutionContext<S> context) {
 *         super(command);
 *         this.context = context;
 *     }
 *
 *     public ExecutionContext<S> getContext() {
 *         return context;
 *     }
 * }
 *
 * // Register a handler that works with any command event
 * eventBus.register(CommandEvent.class, event -> {
 *     Command<?> command = event.getCommand();
 *     logger.info("Command event fired for: " + command.getName());
 * });
 * }</pre>
 *
 * @param <S> the source type that this command supports
 * @since 1.0
 * @author Imperat Framework
 * @see Event
 * @see Command
 * @see EventBus
 */
public abstract class CommandEvent<S extends Source> implements Event {

    /**
     * The command associated with this event.
     */
    protected final Command<S> command;

    /**
     * Constructs a new command event.
     *
     * @param command the command associated with this event
     */
    protected CommandEvent(Command<S> command) {
        this.command = command;
    }

    /**
     * Gets the command associated with this event.
     *
     * @return the command instance
     */
    public Command<S> getCommand() {
        return command;
    }
}