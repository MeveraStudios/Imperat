package studio.mevera.imperat.config.registries;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.events.exception.EventException;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.CommandExceptionHandler;
import studio.mevera.imperat.exception.InvalidSyntaxException;
import studio.mevera.imperat.exception.PermissionDeniedException;
import studio.mevera.imperat.exception.ResponseException;
import studio.mevera.imperat.permissions.PermissionHolder;
import studio.mevera.imperat.responses.ResponseRegistry;
import studio.mevera.imperat.util.ImperatDebugger;
import studio.mevera.imperat.util.UsageFormatting;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores the {@link CommandExceptionHandler}s registered for a given source
 * type. Lookup walks the throwable's superclass chain so a handler registered
 * for a base type covers its subtypes.
 *
 * @param <S> the command-source type
 */
public final class ErrorHandlerRegistry<S extends CommandSource> {

    private final Map<Class<? extends Throwable>, CommandExceptionHandler<?, S>> handlers = new HashMap<>();

    /**
     * Construct a registry pre-populated with the framework's default
     * handlers (invalid syntax, permission denied, response-driven exceptions,
     * event errors, plain {@code CommandException} fallback). The caller-owned
     * {@link ResponseRegistry} is used to resolve {@link ResponseException}s
     * at handle time.
     */
    public static <S extends CommandSource> ErrorHandlerRegistry<S> createDefault(@NotNull ResponseRegistry responseRegistry) {
        ErrorHandlerRegistry<S> registry = new ErrorHandlerRegistry<>();
        registry.installFrameworkDefaults(responseRegistry);
        return registry;
    }

    private static @Nullable String formatPermissionHolder(@Nullable PermissionHolder holder) {
        if (holder == null) {
            return null;
        }
        if (holder instanceof CommandPathway<?> pathway) {
            return pathway.formatted();
        }
        if (holder instanceof Command<?> command) {
            return command.getName();
        }
        if (holder instanceof studio.mevera.imperat.command.arguments.Argument<?> argument) {
            return argument.format();
        }
        return holder.toString();
    }

    public <T extends Throwable> void register(@NotNull Class<T> type, @NotNull CommandExceptionHandler<T, S> handler) {
        handlers.put(type, handler);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends Throwable> CommandExceptionHandler<T, S> getFor(@NotNull Class<T> type) {
        Class<?> current = type;
        while (current != null && Throwable.class.isAssignableFrom(current)) {
            CommandExceptionHandler<?, S> handler = handlers.get(current);
            if (handler != null) {
                return (CommandExceptionHandler<T, S>) handler;
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private void installFrameworkDefaults(@NotNull ResponseRegistry responseRegistry) {
        register(InvalidSyntaxException.class, (exception, ctx) -> {
            String invalidUsageFormat = exception.getInvalidUsage();
            ctx.source().error("Invalid command usage: '" + invalidUsageFormat + "'");

            CommandPathway<?> closestUsage = exception.getClosestUsage();
            if (closestUsage != null) {
                String closestUsageFormat = UsageFormatting.formatClosestUsage(
                        ctx.imperatConfig().commandPrefix(),
                        ctx.getRootCommandLabelUsed(),
                        closestUsage
                );
                ctx.source().error("You probably meant '" + closestUsageFormat + "'");
            }
        });

        register(PermissionDeniedException.class, (exception, context) -> {
            String pathwayFormatted = CommandPathway.format(exception.getLabel(), exception.getExecutingPathway());
            String message = "You don't have permission to execute: '" + pathwayFormatted + "'";
            String deniedTarget = formatPermissionHolder(exception.getPermissionIssuer());
            if (deniedTarget != null && !deniedTarget.isBlank()) {
                message += " (denied by " + deniedTarget + ")";
            }
            context.source().error(message);
        });

        register(ResponseException.class, (exception, context) -> {
            var response = responseRegistry.getResponse(exception.getResponseKey());
            if (response != null) {
                response.sendContent(context, exception.getPlaceholderDataProvider());
            }
        });

        register(EventException.class, (ex, ctx) -> {
            if (ImperatDebugger.isEnabled()) {
                ImperatDebugger.debug(ex.getMessage());
            }
        });

        register(CommandException.class, (exception, context) ->
                                                 context.source().reply(exception.getMessage()));
    }
}
