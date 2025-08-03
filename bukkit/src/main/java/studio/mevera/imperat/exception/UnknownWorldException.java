package studio.mevera.imperat.exception;

public class UnknownWorldException extends ImperatException {

    private final String name;

    public UnknownWorldException(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
