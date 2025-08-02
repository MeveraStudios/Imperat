package dev.velix.imperat.command;

import dev.velix.imperat.Imperat;
import dev.velix.imperat.context.ExecutionContext;
import dev.velix.imperat.context.Source;
import dev.velix.imperat.exception.ImperatException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    ) throws ImperatException;

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
