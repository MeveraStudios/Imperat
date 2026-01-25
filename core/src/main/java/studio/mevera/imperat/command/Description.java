package studio.mevera.imperat.command;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Description {

    public final static Description EMPTY = Description.of(null);
    private final @Nullable String value;

    Description(@Nullable String value) {
        this.value = value;
    }

    public static Description of(String value) {
        return new Description(value);
    }

    public @Nullable String getValue() {
        return value;
    }

    public @NotNull String getValueOrElse(@NotNull String def) {
        return getValue() != null ? getValue() : def;
    }

    public boolean isEmpty() {
        return this == EMPTY || this.value == null || this.value.isBlank();
    }
}
