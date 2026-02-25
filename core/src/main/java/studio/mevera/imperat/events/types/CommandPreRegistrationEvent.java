package studio.mevera.imperat.events.types;

import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.events.CancellableEvent;
import studio.mevera.imperat.events.CommandEvent;

/**
 * Event fired before a command is registered to the command dispatcher.
 *
 * <p>This cancellable event allows listeners to intercept and potentially prevent
 * command registration, enabling dynamic command filtering, validation, and
 * conditional registration based on runtime conditions.</p>
 *
 * <p>When this event is cancelled by setting {@link #setCancelled(boolean)} to
 * {@code true}, the command will not be registered to the dispatcher, and a
 * {@link CommandPostRegistrationEvent} will still be fired with failure information.</p>
 *
 * <h2>Common Use Cases</h2>
 * <ul>
 *     <li>Preventing specific commands from being registered based on configuration</li>
 *     <li>Implementing permission-based command filtering at registration time</li>
 *     <li>Logging and monitoring command registrations for debugging</li>
 *     <li>Validating command metadata before allowing registration</li>
 *     <li>Dynamically enabling/disabling commands based on server state</li>
 *     <li>Preventing conflicting commands from being registered</li>
 * </ul>
 *
 * <h2>Event Lifecycle</h2>
 * <ol>
 *     <li>{@code CommandPreRegistrationEvent} is fired (cancellable)</li>
 *     <li>If not cancelled, command is registered to the dispatcher</li>
 *     <li>{@link CommandPostRegistrationEvent} is fired with success/failure status</li>
 * </ol>
 *
 * <h2>Example: Blocking Specific Commands</h2>
 * <pre>{@code
 * eventBus.register(CommandPreRegistrationEvent.class, event -> {
 *     RootCommand<?> command = event.getCommand();
 *
 *     // Prevent registration of dangerous commands
 *     if (command.getName().equals("dangerous-command")) {
 *         event.setCancelled(true);
 *         logger.warn("Blocked registration of dangerous command");
 *     }
 * });
 * }</pre>
 *
 * <h2>Example: Configuration-Based Filtering</h2>
 * <pre>{@code
 * eventBus.register(CommandPreRegistrationEvent.class, event -> {
 *     RootCommand<?> command = event.getCommand();
 *
 *     // Only allow commands listed in configuration
 *     if (!config.isCommandEnabled(command.getName())) {
 *         event.setCancelled(true);
 *         logger.info("RootCommand '" + command.getName() + "' is disabled in config");
 *     }
 * }, Priority.HIGH);
 * }</pre>
 *
 * <h2>Example: Permission-Based Registration</h2>
 * <pre>{@code
 * eventBus.register(CommandPreRegistrationEvent.class, event -> {
 *     RootCommand<?> command = event.getCommand();
 *
 *     // Check if the server has permission to register this command
 *     if (command.getPermission() != null && !serverHasPermission(command.getPermission())) {
 *         event.setCancelled(true);
 *         logger.warn("Server lacks permission to register: " + command.getName());
 *     }
 * });
 * }</pre>
 *
 * <h2>Example: Logging and Monitoring</h2>
 * <pre>{@code
 * eventBus.register(CommandPreRegistrationEvent.class, event -> {
 *     RootCommand<?> command = event.getCommand();
 *
 *     logger.info("Registering command: " + command.getName());
 *     logger.debug("  Aliases: " + command.getAliases());
 *     logger.debug("  Permission: " + command.getPermission());
 *
 *     // Track registration metrics
 *     metrics.incrementCommandRegistrations();
 * }, Priority.LOWEST); // Run last to ensure other handlers run first
 * }</pre>
 *
 * @param <S> the source type that this command supports
 * @since 1.0
 * @author Imperat Framework
 * @see CommandEvent
 * @see CancellableEvent
 * @see CommandPostRegistrationEvent
 * @see Command
 */
public final class CommandPreRegistrationEvent<S extends Source> extends CommandEvent<S> implements CancellableEvent {

    /**
     * Whether this event has been cancelled.
     */
    private boolean cancelled;

    /**
     * Constructs a new command pre-registration event.
     *
     * @param command the command that is about to be registered
     */
    public CommandPreRegistrationEvent(Command<S> command) {
        super(command);
    }

    /**
     * Checks whether the command registration has been canceled.
     *
     * @return {@code true} if the command registration should be prevented, {@code false} otherwise
     */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Sets whether the command registration should be cancelled.
     *
     * <p>If set to {@code true}, the command will not be registered to the dispatcher,
     * and a {@link CommandPostRegistrationEvent} will be fired indicating the failure.</p>
     *
     * @param cancelled {@code true} to prevent the command from being registered, {@code false} to allow it
     */
    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }


}
