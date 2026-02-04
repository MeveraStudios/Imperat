package studio.mevera.imperat.exception;

public class InvalidUUIDException extends ParseException {

    public InvalidUUIDException(final String raw) {
        super(raw);
    }

}
