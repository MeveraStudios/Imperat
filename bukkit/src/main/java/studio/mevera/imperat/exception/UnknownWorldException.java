package studio.mevera.imperat.exception;

public class UnknownWorldException extends CommandException {

    private final String name;

    public UnknownWorldException(final String name) {
        super();
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
