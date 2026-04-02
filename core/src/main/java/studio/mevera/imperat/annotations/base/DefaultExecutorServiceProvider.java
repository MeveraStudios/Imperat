package studio.mevera.imperat.annotations.base;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

public final class DefaultExecutorServiceProvider implements ExecutorServiceProvider {

    @Override
    public ExecutorService provideExecutorService() {
        return ForkJoinPool.commonPool();
    }
}
