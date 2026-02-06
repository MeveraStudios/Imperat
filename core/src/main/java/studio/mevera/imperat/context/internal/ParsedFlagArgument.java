package studio.mevera.imperat.context.internal;

import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.parameters.FlagArgument;
import studio.mevera.imperat.context.ParsedArgument;
import studio.mevera.imperat.context.Source;

import java.util.Objects;

public final class ParsedFlagArgument<S extends Source> extends ParsedArgument<S> {

    private final @Nullable String flagRawInput;
    private final int flagPosition, inputPosition;

    private ParsedFlagArgument(
            FlagArgument<S> flag,
            @Nullable String flagRaw,
            @Nullable String flagRawInput,
            int flagPosition,
            int inputPosition,
            Object inputValue
    ) {
        super(flagRaw, flag, flag.position(), inputValue);
        this.flagRawInput = flagRawInput;
        this.flagPosition = flagPosition;
        this.inputPosition = inputPosition;
    }
    private ParsedFlagArgument(
            FlagArgument<S> flag,
            String flagRaw,
            int flagPosition
    ) {
        this(flag, flagRaw, null, flagPosition, -1, false);
    }

    public static <S extends Source> ParsedFlagArgument<S> forFlag(
            FlagArgument<S> flag,
            String flagRaw,
            String flagRawInput,
            int flagPosition,
            int inputPosition,
            Object inputValue
    ) {
        return new ParsedFlagArgument<>(flag, flagRaw, flagRawInput, flagPosition, inputPosition, inputValue);
    }

    public static <S extends Source> ParsedFlagArgument<S> forDefaultFlag(
            FlagArgument<S> flag,
            String defaultValueRaw,
            Object defaultParsedValue
    ) {
        return new ParsedFlagArgument<>(flag, "", defaultValueRaw, -1, -1, defaultParsedValue);
    }

    public static <S extends Source> ParsedFlagArgument<S> forSwitch(
            FlagArgument<S> flag,
            String flagRaw,
            int flagPosition
    ) {
        return new ParsedFlagArgument<>(flag, flagRaw, flagPosition);
    }

    public static <S extends Source> ParsedFlagArgument<S> forDefaultSwitch(
            FlagArgument<S> flag
    ) {
        return new ParsedFlagArgument<>(flag, "", -1);
    }

    /**
     * Checks if this flag argument is a default value (i.e. not present in the raw input)
     * @return true if this flag argument is a default value, false otherwise
     */
    public boolean isDefault() {
        return flagPosition == -1;
    }


    /**
     * Checks if this flag argument is a switch (i.e. has no input value)
     * @return true if this flag argument is a switch, false otherwise
     */
    public boolean isSwitch() {
        return this.getOriginalArgument()
                       .asFlagParameter()
                       .isSwitch();
    }

    /**
     * The raw input for this flag argument, e.g. "--flag=value" or "-f value"
     * @return the raw input for this flag argument, empty/blank if this flag argument is a default value (i.e. not present in the raw input)
     */
    @Override
    public @Nullable String getArgumentRawInput() {
        return super.getArgumentRawInput();
    }

    /**
     * The raw input for the flag, e.g. "--flag" or "-f"
     * @return the raw input for the flag
     */
    public @Nullable String getFlagRawInput() {
        return flagRawInput;
    }


    /**
     * The position of the flag in the raw-input arguments from {@link studio.mevera.imperat.context.ArgumentInput}
     * @return the position of the flag in the input
     */
    public int getFlagPosition() {
        return flagPosition;
    }

    /**
     * The position of the input value for this flag, or -1 if this flag is a switch
     * @return the position of the input value for this flag, or -1 if this flag is a switch
     */
    @Override
    public int getInputPosition() {
        return inputPosition;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ParsedFlagArgument<?> that)) {
            return false;
        }
        return Objects.equals(originalArgument, that.originalArgument)
                       && flagPosition == that.flagPosition
                       && inputPosition == that.inputPosition
                       && Objects.equals(flagRawInput, that.flagRawInput)
                       && Objects.equals(getArgumentParsedValue(), that.getArgumentParsedValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                originalArgument, flagRawInput,
                flagPosition, inputPosition, getArgumentParsedValue()
        );
    }
}
