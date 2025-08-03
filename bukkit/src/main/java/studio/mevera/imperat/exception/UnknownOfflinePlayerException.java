package studio.mevera.imperat.exception;

public class UnknownOfflinePlayerException extends ImperatException {

    private final String name;

    public UnknownOfflinePlayerException(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
