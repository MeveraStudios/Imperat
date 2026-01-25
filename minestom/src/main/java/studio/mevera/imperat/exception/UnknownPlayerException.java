package studio.mevera.imperat.exception;


public class UnknownPlayerException extends CommandException {

    private final String name;

    public UnknownPlayerException(final String name) {
        super();
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
