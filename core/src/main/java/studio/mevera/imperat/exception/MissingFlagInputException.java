package studio.mevera.imperat.exception;

import studio.mevera.imperat.command.parameters.FlagParameter;
import studio.mevera.imperat.context.Context;

public class MissingFlagInputException extends ParseException {

    private final FlagParameter<?> flagData;

    public MissingFlagInputException(FlagParameter<?> flagData, String rawFlagEntered, Context<?> ctx) {
        super(rawFlagEntered, ctx);
        this.flagData = flagData;
    }

    public FlagParameter<?> getFlagData() {
        return flagData;
    }

}
