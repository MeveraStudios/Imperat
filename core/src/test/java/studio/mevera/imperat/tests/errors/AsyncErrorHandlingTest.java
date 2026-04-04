package studio.mevera.imperat.tests.errors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.annotations.base.ExecutorServiceProvider;
import studio.mevera.imperat.annotations.types.Async;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.context.ExecutionResult;
import studio.mevera.imperat.tests.TestCommandSource;
import studio.mevera.imperat.tests.TestImperat;
import studio.mevera.imperat.tests.TestImperatConfig;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

class AsyncErrorHandlingTest {

    @Test
    @DisplayName("Should report async root pathway failures through Imperat error handling")
    void testAsyncRootFailureIsReported() throws InterruptedException {
        AtomicReference<Throwable> captured = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        TestImperat imperat = TestImperatConfig.builder()
                                      .permissionChecker((src, perm) -> true)
                                      .throwablePrinter((throwable) -> {
                                          captured.set(throwable);
                                          latch.countDown();
                                      })
                                      .build();

        imperat.registerCommand(AsyncFailingRootCommand.class);

        TestCommandSource source = new TestCommandSource(new PrintStream(new ByteArrayOutputStream()));
        ExecutionResult<TestCommandSource> result = imperat.execute(source, "asyncroot");

        assertFalse(result.hasFailed(), "Async scheduling should not fail synchronously");
        assertTrue(latch.await(2, TimeUnit.SECONDS), "Expected async failure to be reported");

        Throwable throwable = captured.get();
        assertNotNull(throwable, "Reported throwable should not be null");
        assertInstanceOf(IllegalStateException.class, throwable);
        assertEquals("async-root-failure", throwable.getMessage());
    }

    @RootCommand("asyncroot")
    public static final class AsyncFailingRootCommand {

        @Execute
        @Async(TestExecutorServiceProvider.class)
        public void run(TestCommandSource source) {
            throw new IllegalStateException("async-root-failure");
        }
    }

    public static final class TestExecutorServiceProvider implements ExecutorServiceProvider {

        private static final ThreadFactory THREAD_FACTORY = runnable -> {
            Thread thread = new Thread(runnable, "imperat-async-error-test");
            thread.setDaemon(true);
            return thread;
        };

        private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(THREAD_FACTORY);

        @Override
        public ExecutorService provideExecutorService() {
            return EXECUTOR;
        }
    }
}
