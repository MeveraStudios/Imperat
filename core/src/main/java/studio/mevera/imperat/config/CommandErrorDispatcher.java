package studio.mevera.imperat.config;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.ThrowablePrinter;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.config.registries.ErrorHandlerRegistry;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.exception.CommandExceptionHandler;
import studio.mevera.imperat.exception.SelfHandlingException;
import studio.mevera.imperat.util.ImperatDebugger;

/**
 * Owns the error-dispatch pipeline previously inlined in
 * {@code ImperatConfigImpl#handleExecutionError}. Walks (in order):
 *
 * <ol>
 *   <li>The originating command's parent chain — each command may register
 *       its own handlers via {@code Command#handleExecutionError}.</li>
 *   <li>The throwable's cause chain against the global
 *       {@link ErrorHandlerRegistry}, with priority for
 *       {@link SelfHandlingException}s.</li>
 *   <li>Fallback: prints the throwable via the configured
 *       {@link ThrowablePrinter}.</li>
 * </ol>
 *
 * <p>Stateless w.r.t. the throwable being processed; one instance per config.</p>
 *
 * @param <S> the command-source type
 */
public final class CommandErrorDispatcher<S extends CommandSource> {

    private final ErrorHandlerRegistry<S> registry;

    public CommandErrorDispatcher(@NotNull ErrorHandlerRegistry<S> registry) {
        this.registry = registry;
    }

    /**
     * Dispatches {@code throwable}, returning {@code true} if any handler
     * accepted it (commands or global), or after the printer has printed it
     * as a fallback. The legacy contract is "return true once the framework
     * has done all it can" — preserved here unchanged.
     */
    public <E extends Throwable> boolean dispatch(
            @NotNull E throwable,
            @NotNull CommandContext<S> context,
            Class<?> owning,
            String methodName,
            @NotNull ThrowablePrinter printer
    ) {
        if (handleViaCommandChain(throwable, context, owning, methodName)) {
            return true;
        }
        if (handleViaCauseChain(throwable, context)) {
            return true;
        }
        printer.print(throwable);
        return true;
    }

    private <E extends Throwable> boolean handleViaCommandChain(
            E throwable,
            CommandContext<S> context,
            Class<?> owning,
            String methodName
    ) {
        Command<S> cmd = context instanceof ExecutionContext<S> executionContext
                                 ? executionContext.getLastUsedCommand()
                                 : context.command();
        while (cmd != null) {
            if (cmd.handleExecutionError(throwable, context, owning, methodName)) {
                return true;
            }
            cmd = cmd.getParent();
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private <E extends Throwable> boolean handleViaCauseChain(E throwable, CommandContext<S> context) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SelfHandlingException selfHandling) {
                selfHandling.handle(context);
                return true;
            }
            CommandExceptionHandler<? super Throwable, S> handler =
                    (CommandExceptionHandler<? super Throwable, S>) registry.getFor(current.getClass());
            if (handler != null) {
                ImperatDebugger.debug("Found handler for exception '%s'", current.getClass().getName());
                handler.resolve(current, context);
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
