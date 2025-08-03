package studio.mevera.imperat.selector.field;

import studio.mevera.imperat.exception.ImperatException;
import studio.mevera.imperat.exception.SourceException;
import studio.mevera.imperat.util.TypeUtility;
import studio.mevera.imperat.util.TypeWrap;

/**
 * RangedNumericField is a concrete implementation of AbstractField that handles numeric fields with range values.
 * The range is represented using the Range class, which allows specifying minimum and/or maximum bounds.
 *
 * @param <N> The type of the numeric value that this field handles, which extends Number.
 */
public final class RangedNumericField<N extends Number> extends AbstractField<Range<N>> {

    private final static String RANGE_CHARACTER = "\\.\\.";
    private final static String RANGE_CHARACTER_WITHOUT_ESCAPE = "..";

    private final NumericField<N> numericField;

    private RangedNumericField(NumericField<N> numericField) {
        super(numericField.name, new TypeWrap<>() {
        });
        this.numericField = numericField;
    }


    public static <N extends Number> RangedNumericField<N> of(NumericField<N> numericField) {
        return new RangedNumericField<>(numericField);
    }

    /**
     * Parses the given string representation of the value and converts it into the field's value type.
     *
     * @param value the string representation of the value to be parsed
     * @return the parsed value of the field's type
     * @throws ImperatException if the parsing fails
     */
    @Override
    public Range<N> parseFieldValue(String value) throws ImperatException {
        if(!value.contains(RANGE_CHARACTER_WITHOUT_ESCAPE)) {
            N numericValue = numericField.parseNumber(value);
            return Range.atLeast(numericValue);
        }else  {
            if (value.startsWith(RANGE_CHARACTER_WITHOUT_ESCAPE)) {
                //less than or equal to value
                N rangeValue = numericField.parseNumber(value.replace(RANGE_CHARACTER_WITHOUT_ESCAPE, ""));
                return Range.atLeast(rangeValue);
            } else if (value.endsWith(RANGE_CHARACTER_WITHOUT_ESCAPE)) {
                N rangeValue = numericField.parseNumber(value.replace(RANGE_CHARACTER_WITHOUT_ESCAPE, ""));
                return Range.atMost(rangeValue);
            } else  {
                String[] minMaxSplit = value.split(RANGE_CHARACTER);
                if (minMaxSplit.length > 2) {
                    throw new SourceException("Invalid distance range format '%s'", value);
                }
                String minStr = minMaxSplit[0], maxStr = minMaxSplit[1];

                if (!TypeUtility.isNumber(minStr)) {
                    throw new SourceException("Invalid min-value '%s'", minStr);
                }

                if (!TypeUtility.isNumber(maxStr)) {
                    throw new SourceException("Invalid max-value '%s'", maxStr);
                }

                N min = numericField.parseFieldValue(minStr), max = numericField.parseFieldValue(maxStr);
                return Range.of(min, max);
            }
        }
    }

}
