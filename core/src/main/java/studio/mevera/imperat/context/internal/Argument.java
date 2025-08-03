package studio.mevera.imperat.context.internal;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.context.Source;

@ApiStatus.Internal
public record Argument<S extends Source>(
    @Nullable String raw,
    CommandParameter<S> parameter,
    int index, @Nullable Object value
) {
    @Override
    public String toString() {
        return "Argument{" +
            "raw='" + raw + '\'' +
            ", parameter=" + parameter.format() +
            ", index=" + index +
            ", value=" + value +
            '}';
    }
}
