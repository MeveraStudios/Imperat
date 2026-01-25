package studio.mevera.imperat.exception;

public class UnknownOfflinePlayerException extends CommandException {

    private final String name;

    public UnknownOfflinePlayerException(final String name) {
        super();
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
