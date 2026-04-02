package studio.mevera.imperat.annotations.base;

import java.util.concurrent.ExecutorService;

public interface ExecutorServiceProvider {

    /**
     * Provides an {@link java.util.concurrent.ExecutorService} to be used for asynchronous command execution.
     *
     * @return An instance of {@link java.util.concurrent.ExecutorService} or null to use the default executor.
     */
    ExecutorService provideExecutorService();

}
