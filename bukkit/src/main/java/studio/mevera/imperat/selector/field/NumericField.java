package studio.mevera.imperat.selector.field;

import studio.mevera.imperat.BukkitSource;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.SourceException;
import studio.mevera.imperat.util.TypeUtility;
import studio.mevera.imperat.util.TypeWrap;

public sealed abstract class NumericField<N extends Number> extends AbstractField<N>
        permits NumericField.ByteField, NumericField.DoubleField, NumericField.FloatField,
                        NumericField.IntegerField, NumericField.LongField, NumericField.ShortField {

    /**
     * Constructs an AbstractField instance with the specified name and type.
     *
     * @param name The name of the selection field.
     * @param type The type information of the value that this field handles, wrapped in a TypeWrap object.
     */
    protected NumericField(String name, TypeWrap<N> type) {
        super(name, type);
    }

    public static NumericField<Integer> integerField(String name) {
        return new IntegerField(name);
    }

    public static NumericField<Double> doubleField(String name) {
        return new DoubleField(name);
    }

    public static NumericField<Float> floatField(String name) {
        return new FloatField(name);
    }

    public static NumericField<Long> longField(String name) {
        return new LongField(name);
    }

    public static NumericField<Short> shortField(String name) {
        return new ShortField(name);
    }

    public static NumericField<Byte> byteField(String name) {
        return new ByteField(name);
    }

    /**
     * Parses the given string representation of the value and converts it into the field's value type.
     *
     * @param value   the string representation of the value to be parsed
     * @param context the context
     * @return the parsed value of the field's type
     * @throws CommandException if the parsing fails
     */
    @Override
    public N parseFieldValue(String value, Context<BukkitSource> context) throws CommandException {
        return parseNumber(value, context);
    }

    protected abstract N parseNumber(String value, Context<BukkitSource> ctx) throws CommandException;

    final static class IntegerField extends NumericField<Integer> {

        public IntegerField(String name) {
            super(name, TypeWrap.of(Integer.class));
        }

        @Override
        public Integer parseNumber(String value, Context<BukkitSource> ctx) throws CommandException {
            if (!TypeUtility.isInteger(value)) {
                throw new SourceException("Invalid limit-value integer '%s'", value);
            }
            return Integer.parseInt(value);
        }
    }

    final static class DoubleField extends NumericField<Double> {

        public DoubleField(String name) {
            super(name, TypeWrap.of(Double.class));
        }

        @Override
        public Double parseNumber(String value, Context<BukkitSource> ctx) throws CommandException {
            if (!TypeUtility.isDouble(value)) {
                throw new SourceException("Invalid double value '%s'", value);
            }
            return Double.parseDouble(value);
        }
    }

    final static class FloatField extends NumericField<Float> {

        public FloatField(String name) {
            super(name, TypeWrap.of(Float.class));
        }

        @Override
        public Float parseNumber(String value, Context<BukkitSource> ctx) throws CommandException {
            if (!TypeUtility.isFloat(value)) {
                throw new SourceException("Invalid float value '%s'", value);
            }
            return Float.parseFloat(value);
        }
    }

    final static class LongField extends NumericField<Long> {

        public LongField(String name) {
            super(name, TypeWrap.of(Long.class));
        }

        @Override
        public Long parseNumber(String value, Context<BukkitSource> ctx) throws CommandException {
            if (!TypeUtility.isLong(value)) {
                throw new SourceException("Invalid long value '%s'", value);
            }
            return Long.parseLong(value);
        }
    }

    final static class ShortField extends NumericField<Short> {

        public ShortField(String name) {
            super(name, TypeWrap.of(Short.class));
        }

        @Override
        public Short parseNumber(String value, Context<BukkitSource> ctx) throws CommandException {
            if (!TypeUtility.isShort(value)) {
                throw new SourceException("Invalid short value '%s'", value);
            }
            return Short.parseShort(value);
        }

    }

    final static class ByteField extends NumericField<Byte> {

        public ByteField(String name) {
            super(name, TypeWrap.of(Byte.class));
        }

        @Override
        public Byte parseNumber(String value, Context<BukkitSource> ctx) throws CommandException {
            if (!TypeUtility.isByte(value)) {
                throw new SourceException("Invalid byte value '%s'", value);
            }
            return Byte.parseByte(value);
        }
    }
}
