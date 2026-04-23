package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.types.ExceptionHandler;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.tests.TestCommandSource;
import studio.mevera.imperat.tests.parameters.ParseFailedException;
import studio.mevera.imperat.tests.parameters.ThrowingValue;

/**
 * Regression command for the bug where a typed {@link CommandException} thrown from
 * {@code ArgumentType#parse} was swallowed and replaced with a generic
 * {@code InvalidSyntaxException}, preventing any command-specific
 * {@link ExceptionHandler} from matching the original exception type.
 */
@RootCommand("throwparse")
public class ThrowingParseCommand {

    public static volatile String lastHandledMessage = null;
    public static volatile Class<?> lastHandledType = null;

    @Execute
    public void run(TestCommandSource source, @Named("value") ThrowingValue value) {
        source.reply("Got: " + value.raw());
    }

    @ExceptionHandler(ParseFailedException.class)
    public void handleParseFailed(ParseFailedException exception, CommandContext<TestCommandSource> context) {
        lastHandledMessage = exception.getMessage();
        lastHandledType = ParseFailedException.class;
        context.source().reply("Caught locally: " + exception.getMessage());
    }
}
