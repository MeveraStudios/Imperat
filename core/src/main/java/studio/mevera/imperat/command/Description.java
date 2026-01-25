package studio.mevera.imperat.command;

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

    @Override
    public String toString() {
        return value;
    }

    public boolean isEmpty() {
        return this == EMPTY || this.value == null || this.value.isBlank();
    }
}
