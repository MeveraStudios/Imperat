package studio.mevera.imperat.exception;

public class UnknownPlayerException extends ParseException {

    public UnknownPlayerException(final String name) {
        super(name);
    }

}
