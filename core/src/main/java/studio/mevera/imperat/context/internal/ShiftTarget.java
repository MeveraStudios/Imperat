package studio.mevera.imperat.context.internal;

public enum ShiftTarget {

    RAW_ONLY((pos) -> pos.raw < pos.maxRaws()),

    PARAMETER_ONLY((pos) -> pos.parameter < pos.maxParameters()),

    ALL((pos) ->
                pos.raw < pos.maxRaws() && pos.parameter < pos.maxParameters());

    private final PositionShiftCondition canContinueCheck;

    ShiftTarget(PositionShiftCondition canContinueCheck) {
        this.canContinueCheck = canContinueCheck;
    }

    boolean canContinue(CursorPosition<?> cursorPosition) {
        return canContinueCheck.canContinue(cursorPosition);
    }

}