package studio.mevera.imperat.exception.parse;

import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.exception.ParseException;
import studio.mevera.imperat.util.TypeWrap;

public class InvalidNumberFormatException extends ParseException {

    private final @Nullable NumberFormatException originalError;
    private final String numberTypeDisplay;
    private final TypeWrap<? extends Number> numericType;


    public InvalidNumberFormatException(
            String input, @Nullable NumberFormatException originalError,
            String numberTypeDisplay,
            TypeWrap<? extends Number> numericType,
            Context<?> ctx
    ) {
        super(input, ctx);
        this.originalError = originalError;
        this.numberTypeDisplay = numberTypeDisplay;
        this.numericType = numericType;
    }

    public String getNumberTypeDisplay() {
        return numberTypeDisplay;
    }

    public TypeWrap<? extends Number> getNumericType() {
        return numericType;
    }

    public @Nullable NumberFormatException getOriginalError() {
        return originalError;
    }
}
