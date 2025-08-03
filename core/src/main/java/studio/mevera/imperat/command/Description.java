package studio.mevera.imperat.command;

public final class Description {

    private final static String NON_APPLICABLE = "N/A";
    public final static Description EMPTY = Description.of(NON_APPLICABLE);
    private final String value;

    Description(String value) {
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
        return this == EMPTY || this.value.isBlank() || this.value.equals(NON_APPLICABLE);
    }
}
