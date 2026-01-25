package studio.mevera.imperat.exception;

import studio.mevera.imperat.command.parameters.FlagParameter;

public class MissingFlagInputException extends ParseException {

    private final FlagParameter<?> flagData;

    public MissingFlagInputException(FlagParameter<?> flagData, String rawFlagEntered) {
        super(rawFlagEntered);
        this.flagData = flagData;
    }

    public FlagParameter<?> getFlagData() {
        return flagData;
    }

}
