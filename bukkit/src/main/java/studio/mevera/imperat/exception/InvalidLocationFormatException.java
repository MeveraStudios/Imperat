package studio.mevera.imperat.exception;

import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.context.Context;

public class InvalidLocationFormatException extends ParseException {

    private final Reason reason;
    private final @Nullable String inputX, inputY, inputZ, inputPitch, inputYaw;

    public InvalidLocationFormatException(
            String input,
            Reason reason,
            @Nullable String inputX,
            @Nullable String inputY,
            @Nullable String inputZ,
            @Nullable String inputPitch,
            @Nullable String inputYaw,
            Context<?> ctx
    ) {
        super(input, ctx);
        this.reason = reason;
        this.inputX = inputX;
        this.inputY = inputY;
        this.inputZ = inputZ;
        this.inputPitch = inputPitch;
        this.inputYaw = inputYaw;
    }

    public InvalidLocationFormatException(String input, Reason reason, Context<?> ctx) {
        this(input, reason, null, null, null, null, null, ctx);
    }

    public Reason getReason() {
        return reason;
    }

    public @Nullable String getInputX() {
        return inputX;
    }

    public @Nullable String getInputY() {
        return inputY;
    }

    public @Nullable String getInputZ() {
        return inputZ;
    }

    public @Nullable String getInputYaw() {
        return inputYaw;
    }

    public @Nullable String getInputPitch() {
        return inputPitch;
    }

    public enum Reason {

        INVALID_X_COORDINATE,

        INVALID_Y_COORDINATE,

        INVALID_Z_COORDINATE,

        INVALID_YAW_COORDINATE,

        INVALID_PITCH_COORDINATE,

        NO_WORLDS_AVAILABLE,

        WRONG_FORMAT,

        SELF_LOCATION_NOT_AVAILABLE

    }

}