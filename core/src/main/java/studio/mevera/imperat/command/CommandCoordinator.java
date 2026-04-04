package studio.mevera.imperat.command;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.Imperat;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.exception.CommandException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public interface CommandCoordinator<S extends CommandSource> {

    static <S extends CommandSource> CommandCoordinator<S> sync() {
        return (api, source, context, execution) -> {
            execution.execute(source, context);
        };
    }

    static <S extends CommandSource> CommandCoordinator<S> async(final @NotNull ExecutorService executorService) {
        return ((api, source, context, execution) ->
                        CompletableFuture.runAsync(() -> {
                            try {
                                execution.execute(source, context);
                            } catch (Throwable throwable) {
                                api.config().handleExecutionError(
                                        throwable,
                                        context,
                                        CommandCoordinator.class,
                                        "async(executorService)"
                                );
                            }
                        }, executorService));
    }

    void coordinate(
            @NotNull Imperat<S> imperat,
            @NotNull S source,
            @NotNull ExecutionContext<S> context,
            @NotNull CommandExecution<S> execution
    ) throws CommandException;

    @FunctionalInterface
    interface UnsafeRunnable extends Runnable {

        void runUnsafe() throws Throwable;

        @Override
        default void run() {
            try {
                runUnsafe();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }
}
