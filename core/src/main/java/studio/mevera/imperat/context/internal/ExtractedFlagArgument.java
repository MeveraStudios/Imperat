package studio.mevera.imperat.context.internal;

import studio.mevera.imperat.context.FlagData;

public record ExtractedFlagArgument(FlagData<?> flag, String flagRaw,
                                    String flagRawInput, Object value) {

    public boolean isSwitch() {
        return flag.inputType() == null;
    }

}
