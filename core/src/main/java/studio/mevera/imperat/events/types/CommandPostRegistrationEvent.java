package studio.mevera.imperat.events.types;

import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.events.CommandEvent;

/**
 * Event fired after a command registration attempt completes, whether successful or failed.
 *
 * <p>This event is always fired following a {@link CommandPreRegistrationEvent}, and provides
 * information about the outcome of the registration attempt. Unlike the pre-registration event,
 * this event is not cancellable as the registration has already been attempted.</p>
 *
 * <p>The event can indicate either success or failure:</p>
 * <ul>
 *     <li><b>Success:</b> {@link #isSuccessful()} returns {@code true}, {@link #getFailureCause()} returns {@code null}</li>
 *     <li><b>Failure:</b> {@link #isFailed()} returns {@code true}, {@link #getFailureCause()} contains the exception</li>
 * </ul>
 *
 * <h2>Common Use Cases</h2>
 * <ul>
 *     <li>Logging successful command registrations for audit trails</li>
 *     <li>Handling and reporting registration failures gracefully</li>
 *     <li>Tracking command registration metrics and statistics</li>
 *     <li>Performing post-registration setup or initialization</li>
 *     <li>Notifying external systems of new command availability</li>
 *     <li>Debugging registration issues with detailed error information</li>
 * </ul>
 *
 * <h2>Event Lifecycle</h2>
 * <ol>
 *     <li>{@link CommandPreRegistrationEvent} is fired (cancellable)</li>
 *     <li>If not cancelled, command registration is attempted</li>
 *     <li>{@code CommandPostRegistrationEvent} is fired with the result</li>
 * </ol>
 *
 * <h2>Example: Success/Failure Logging</h2>
 * <pre>{@code
 * eventBus.register(CommandPostRegistrationEvent.class, event -> {
 *     RootCommand<?> command = event.getCommand();
 *
 *     if (event.isSuccessful()) {
 *         logger.info("Successfully registered command: " + command.getName());
 *     } else {
 *         logger.error("Failed to register command: " + command.getName(),
 *                      event.getFailureCause());
 *     }
 * });
 * }</pre>
 *
 * <h2>Example: Registration Metrics</h2>
 * <pre>{@code
 * eventBus.register(CommandPostRegistrationEvent.class, event -> {
 *     if (event.isSuccessful()) {
 *         metrics.incrementSuccessfulRegistrations();
 *         metrics.recordCommandRegistered(event.getCommand().getName());
 *     } else {
 *         metrics.incrementFailedRegistrations();
 *         metrics.recordRegistrationError(event.getCommand().getName(),
 *                                         event.getFailureCause());
 *     }
 * });
 * }</pre>
 *
 * <h2>Example: Post-Registration Setup</h2>
 * <pre>{@code
 * eventBus.register(CommandPostRegistrationEvent.class, event -> {
 *     if (event.isSuccessful()) {
 *         RootCommand<?> command = event.getCommand();
 *
 *         // Perform additional setup after successful registration
 *         registerCommandAliases(command);
 *         setupCommandPermissions(command);
 *         notifyAdmins("New command available: " + command.getName());
 *     }
 * });
 * }</pre>
 *
 * <h2>Example: Error Recovery</h2>
 * <pre>{@code
 * eventBus.register(CommandPostRegistrationEvent.class, event -> {
 *     if (event.isFailed()) {
 *         RootCommand<?> command = event.getCommand();
 *         Throwable cause = event.getFailureCause();
 *
 *         // Attempt recovery strategies
 *         if (cause instanceof CommandConflictException) {
 *             logger.warn("RootCommand conflict for: " + command.getName() +
 *                         ", attempting to register with prefix");
 *             registerWithPrefix(command);
 *         } else {
 *             logger.error("Unrecoverable registration error", cause);
 *             alertAdministrators(command, cause);
 *         }
 *     }
 * });
 * }</pre>
 *
 * <h2>Example: Notification System</h2>
 * <pre>{@code
 * eventBus.register(CommandPostRegistrationEvent.class, event -> {
 *     RootCommand<?> command = event.getCommand();
 *
 *     if (event.isSuccessful()) {
 *         // Notify monitoring system
 *         monitoringService.recordEvent(
 *             "command.registered",
 *             Map.of(
 *                 "command", command.getName(),
 *                 "aliases", String.join(",", command.getAliases()),
 *                 "permission", command.getPermission()
 *             )
 *         );
 *     }
 * }, Priority.LOWEST, ExecutionStrategy.ASYNC);
 * }</pre>
 *
 * @param <S> the source type that this command supports
 * @since 1.0
 * @author Imperat Framework
 * @see CommandEvent
 * @see CommandPreRegistrationEvent
 * @see Command
 */
public final class CommandPostRegistrationEvent<S extends Source> extends CommandEvent<S> {

    /**
     * The exception that caused the registration to fail, or {@code null} if successful.
     */
    private @Nullable Throwable failureCause;

    /**
     * Constructs a new command post-registration event.
     *
     * @param command the command that was attempted to be registered
     * @param failureCause the exception that caused the failure, or {@code null} if successful
     */
    public CommandPostRegistrationEvent(Command<S> command, @Nullable Throwable failureCause) {
        super(command);
        this.failureCause = failureCause;
    }

    /**
     * Checks if the command registration was successful.
     *
     * @return {@code true} if the command was registered successfully, {@code false} if it failed
     */
    public boolean isSuccessful() {
        return failureCause == null;
    }

    /**
     * Checks if the command registration failed.
     *
     * @return {@code true} if the command registration failed, {@code false} if it was successful
     */
    public boolean isFailed() {
        return failureCause != null;
    }

    /**
     * Sets the exception that caused the registration to fail.
     *
     * @param failureCause the failure cause, or {@code null} if the registration was successful
     */
    public void setFailureCause(@Nullable Throwable failureCause) {
        this.failureCause = failureCause;
    }

    /**
     * Gets the exception that caused the registration to fail.
     *
     * @return the failure cause, or {@code null} if the registration was successful
     */
    public @Nullable Throwable getFailureCause() {
        return failureCause;
    }
}
