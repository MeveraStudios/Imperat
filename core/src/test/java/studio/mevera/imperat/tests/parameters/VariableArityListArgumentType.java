package studio.mevera.imperat.tests.parameters;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.command.arguments.type.Cursor;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.tests.TestCommandSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Tier-3 (variable-arity) argument type — extends {@link ArgumentType}
 * directly, NOT {@link studio.mevera.imperat.command.arguments.type.SimpleArgumentType}
 * and NOT {@link studio.mevera.imperat.command.arguments.type.GreedyArgumentType}.
 *
 * <p>Consumes tokens until it peeks the literal {@code "stop"} sentinel or
 * the cursor is exhausted. Returns the collected tokens as a list. The
 * sentinel itself is NOT consumed — it remains for the next argument.</p>
 */
public final class VariableArityListArgumentType extends ArgumentType<TestCommandSource, List<String>> {

    public VariableArityListArgumentType() {
        super();
    }

    @Override
    public List<String> parse(
            @NotNull CommandContext<TestCommandSource> context,
            @NotNull Argument<TestCommandSource> argument,
            @NotNull Cursor<TestCommandSource> cursor
    ) throws CommandException {
        List<String> collected = new ArrayList<>();
        while (cursor.hasNext()) {
            String peek = cursor.peek();
            if (peek == null || "stop".equalsIgnoreCase(peek)) {
                break;
            }
            collected.add(cursor.next());
        }
        return collected;
    }
}
