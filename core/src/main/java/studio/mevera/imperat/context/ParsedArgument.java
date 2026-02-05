package studio.mevera.imperat.context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.parameters.Argument;

public record ParsedArgument<S extends Source>(
        @Nullable String raw,
        Argument<S> parameter,
        int index,
        @Nullable Object value
) {

    public String name() {
        return parameter.name();
    }

    public <T> @Nullable T getValue() {
        return (T) value;
    }

    @Override
    public @NotNull String toString() {
        return "Argument{" +
                       "raw='" + raw + '\'' +
                       ", parameter=" + parameter.format() +
                       ", index=" + index +
                       ", value=" + value +
                       '}';
    }
}
