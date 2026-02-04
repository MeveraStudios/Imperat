package studio.mevera.imperat.command.parameters.validator;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.Argument;
import studio.mevera.imperat.util.Priority;

/**
 * Validates a parsed command argument.
 *
 * <p>This is a functional interface intended to be used wherever argument\-level validation
 * is required (e.g., to reject missing/invalid values before command execution).</p>
 *
 * @param <S> the {@link Source} type providing execution context for the argument
 */
public interface ArgValidator<S extends Source> extends Comparable<ArgValidator<S>> {

    static <S extends Source> ArgValidator<S> empty() {
        return new ArgValidator<>() {
            @Override
            public @NotNull Priority priority() {
                return Priority.LOW;
            }

            @Override
            public void validate(Context<S> context, Argument<S> argument) throws InvalidArgumentException {
            }
        };
    }

    @NotNull Priority priority();

    void validate(Context<S> context, Argument<S> argument) throws InvalidArgumentException;

    @Override
    default int compareTo(@NotNull ArgValidator<S> o) {
        return Integer.compare(this.priority().getLevel(), o.priority().getLevel());
    }

}