package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.types.ExceptionHandler;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.tests.TestSource;

/**
 * A command that always throws a custom exception, used to test
 * command-specific {@link ExceptionHandler} methods.
 */
@RootCommand("errtest")
public class ExceptionHandlerTestCmd {

    public static volatile String lastHandledMessage = null;
    public static volatile Class<?> lastHandledType = null;

    @Execute
    public void run(TestSource source, String action) throws TestCommandException {
        if ("crash".equalsIgnoreCase(action)) {
            throw new TestCommandException("Something went wrong in errtest");
        }
        source.reply("Action executed: " + action);
    }

    @ExceptionHandler(TestCommandException.class)
    public void handleTestException(TestCommandException exception, CommandContext<TestSource> context) {
        lastHandledMessage = exception.getMessage();
        lastHandledType = TestCommandException.class;
        context.source().reply("Caught locally: " + exception.getMessage());
    }
}

