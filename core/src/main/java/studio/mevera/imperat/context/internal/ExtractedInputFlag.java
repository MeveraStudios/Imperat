package studio.mevera.imperat.context.internal;

import studio.mevera.imperat.context.FlagData;

public record ExtractedInputFlag(FlagData<?> flag, String flagRaw,
                                 String flagRawInput, Object value) {

    public boolean isSwitch() {
        return flag.inputType() == null;
    }

}
