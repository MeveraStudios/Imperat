package studio.mevera.imperat.exception;

public class UnknownPlayerException extends ImperatException {

    private final String name;

    public UnknownPlayerException(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
