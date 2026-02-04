package studio.mevera.imperat.command;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.Imperat;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.CommandException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

public interface CommandCoordinator<S extends Source> {

    static <S extends Source> CommandCoordinator<S> sync() {
        return (api, source, context, execution) -> {
            execution.execute(source, context);
        };
    }

    static <S extends Source> CommandCoordinator<S> async(final @Nullable ExecutorService service) {
        return ((api, source, context, execution) -> {
            ExecutorService executorService = service;
            if (executorService == null) {
                executorService = ForkJoinPool.commonPool();
            }
            CompletableFuture.runAsync((UnsafeRunnable) () -> execution.execute(source, context), executorService);
        });
    }

    static <S extends Source> CommandCoordinator<S> async() {
        return async(null);
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
