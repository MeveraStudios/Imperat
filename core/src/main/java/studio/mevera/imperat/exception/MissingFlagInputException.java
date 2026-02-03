package studio.mevera.imperat.exception;


import java.util.Set;

public class MissingFlagInputException extends ParseException {

    private final Set<String> flagsUsed;

    public MissingFlagInputException(Set<String> flagsUsed, String rawFlagEntered) {
        super(rawFlagEntered);
        this.flagsUsed = flagsUsed;
    }


    public Set<String> getFlagData() {
        return flagsUsed;
    }

}
