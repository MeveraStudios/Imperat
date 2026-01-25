package studio.mevera.imperat.exception;

public class UnknownPlayerException extends ParseException {

    private final String name;

    public UnknownPlayerException(final String name) {
        super(name);
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
