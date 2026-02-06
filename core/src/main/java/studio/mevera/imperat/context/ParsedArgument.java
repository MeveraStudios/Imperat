package studio.mevera.imperat.context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.parameters.Argument;

public class ParsedArgument<S extends Source> {

    protected final @Nullable String raw;
    protected final Argument<S> originalArgument;
    protected final int inputPosition;
    protected final @Nullable Object argumentParsedValue;

    public ParsedArgument(
            @Nullable String raw,
            Argument<S> originalArgument,
            int inputPosition,
            @Nullable Object argumentParsedValue
    ) {
        this.raw = raw;
        this.originalArgument = originalArgument;
        this.inputPosition = inputPosition;
        this.argumentParsedValue = argumentParsedValue;
    }

    public @Nullable String getArgumentRawInput() {
        return raw;
    }

    public Argument<S> getOriginalArgument() {
        return originalArgument;
    }

    public int getInputPosition() {
        return inputPosition;
    }

    public String getArgumentName() {
        return originalArgument.name();
    }

    public <T> @Nullable T getArgumentParsedValue() {
        return (T) argumentParsedValue;
    }

    @Override
    public @NotNull String toString() {
        return "Argument{" +
                       "raw='" + raw + '\'' +
                       ", parameter=" + originalArgument.format() +
                       ", index=" + inputPosition +
                       ", value=" + argumentParsedValue +
                       '}';
    }
}
