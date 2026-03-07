package studio.mevera.imperat.command.arguments.validator;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.ParsedArgument;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.util.priority.Prioritizable;
import studio.mevera.imperat.util.priority.Priority;

/**
 * Validates a parsed command argument.
 *
 * <p>This is a functional interface intended to be used wherever argument\-level validation
 * is required (e.g., to reject missing/invalid values before command execution).</p>
 *
 * @param <S> the {@link CommandSource} type providing execution context for the argument
 */
public interface ArgValidator<S extends CommandSource> extends Comparable<ArgValidator<S>>, Prioritizable {

    static <S extends CommandSource> ArgValidator<S> empty() {
        return new ArgValidator<>() {
            @Override
            public @NotNull Priority getPriority() {
                return Priority.LOW;
            }

            @Override
            public void validate(CommandContext<S> context, ParsedArgument<S> parsedArgument) throws CommandException {
            }
        };
    }

    @Override
    default @NotNull Priority getPriority() {
        return Priority.NORMAL;
    }

    void validate(CommandContext<S> context, ParsedArgument<S> parsedArgument) throws CommandException;

    @Override
    default int compareTo(@NotNull ArgValidator<S> o) {
        return Integer.compare(this.getPriority().getLevel(), o.getPriority().getLevel());
    }

}