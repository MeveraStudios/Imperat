package studio.mevera.imperat.command.parameters.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.responses.ResponseKey;
import studio.mevera.imperat.util.Priority;
import studio.mevera.imperat.util.TypeUtility;

public abstract class NumberArgument<S extends Source, N extends Number> extends ArgumentType<S, N> {

    protected NumberArgument() {
        super();
    }

    @SuppressWarnings("unchecked")
    static <S extends Source, N extends Number> NumberArgument<S, N> from(Class<N> numType) {
        if (TypeUtility.matches(numType, Integer.class)) {
            return (NumberArgument<S, N>) new IntArgument<>();
        } else if (TypeUtility.matches(numType, Long.class)) {
            return (NumberArgument<S, N>) new LongArgument<>();
        } else if (TypeUtility.matches(numType, Float.class)) {
            return (NumberArgument<S, N>) new FloatArgument<>();
        } else if (TypeUtility.matches(numType, Double.class)) {
            return (NumberArgument<S, N>) new DoubleArgument<>();
        } else if (TypeUtility.matches(numType, Byte.class)) {
            return (NumberArgument<S, N>) new ByteArgument<>();
        } else if (TypeUtility.matches(numType, Short.class)) {
            return (NumberArgument<S, N>) new ShortArgument<>();
        } else if (TypeUtility.matches(numType, java.math.BigInteger.class)) {
            return (NumberArgument<S, N>) new BigIntegerArgument<>();
        } else if (TypeUtility.matches(numType, java.math.BigDecimal.class)) {
            return (NumberArgument<S, N>) new BigDecimalArgument<>();
        } else {
            throw new IllegalArgumentException("Unsupported number type: " + numType.getTypeName());
        }
    }

    @Override
    public @Nullable N parse(@NotNull ExecutionContext<S> context, @NotNull Cursor<S> cursor, @NotNull String correspondingInput) throws
            CommandException {
        try {
            return parse(correspondingInput);
        } catch (NumberFormatException ex) {
            throw new CommandException(ResponseKey.INVALID_NUMBER_FORMAT)
                          .withPlaceholder("input", correspondingInput)
                          .withPlaceholder("number_type", display());
        }
    }

    @Override
    public boolean matchesInput(int rawPosition, Context<S> context, Argument<S> parameter) {
        String input = context.arguments().get(rawPosition);
        if (input == null) {
            return false;
        }

        try {
            parse(input);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public abstract String display();

    public abstract N parse(String input) throws NumberFormatException;

    static class IntArgument<S extends Source> extends NumberArgument<S, Integer> {

        protected IntArgument() {
            super();
        }

        @Override
        public String display() {
            return "integer";
        }

        @Override
        public Integer parse(String input) throws NumberFormatException {
            return Integer.parseInt(input);
        }

        @Override
        public Priority priority() {
            return Priority.NORMAL;
        }
    }

    static class FloatArgument<S extends Source> extends NumberArgument<S, Float> {

        protected FloatArgument() {
            super();
        }

        @Override
        public String display() {
            return "float";
        }

        @Override
        public Float parse(String input) throws NumberFormatException {
            return Float.parseFloat(input);
        }

        public Priority priority() {
            return Priority.HIGH;
        }
    }

    static class LongArgument<S extends Source> extends NumberArgument<S, Long> {

        protected LongArgument() {
            super();
        }

        @Override
        public String display() {
            return "long";
        }

        @Override
        public Long parse(String input) throws NumberFormatException {
            return Long.parseLong(input);
        }

        public Priority priority() {
            return Priority.NORMAL.plus(1);
        }
    }

    static class DoubleArgument<S extends Source> extends NumberArgument<S, Double> {

        protected DoubleArgument() {
            super();
        }

        @Override
        public String display() {
            return "double";
        }

        @Override
        public Double parse(String input) throws NumberFormatException {
            return Double.parseDouble(input);
        }

        public Priority priority() {
            return Priority.HIGH.plus(1);
        }
    }

    //create for Byte, Short, BigInteger, BigDecimal
    //do it for me
    static class ByteArgument<S extends Source> extends NumberArgument<S, Byte> {

        protected ByteArgument() {
            super();
        }

        @Override
        public String display() {
            return "byte";
        }

        @Override
        public Byte parse(String input) throws NumberFormatException {
            return Byte.parseByte(input);
        }

        public Priority priority() {
            return Priority.NORMAL;
        }
    }

    static class ShortArgument<S extends Source> extends NumberArgument<S, Short> {

        protected ShortArgument() {
            super();
        }

        @Override
        public String display() {
            return "short";
        }

        @Override
        public Short parse(String input) throws NumberFormatException {
            return Short.parseShort(input);
        }

        public Priority priority() {
            return Priority.NORMAL;
        }
    }

    static class BigIntegerArgument<S extends Source> extends NumberArgument<S, java.math.BigInteger> {

        protected BigIntegerArgument() {
            super();
        }

        @Override
        public String display() {
            return "big integer";
        }

        @Override
        public java.math.BigInteger parse(String input) throws NumberFormatException {
            return new java.math.BigInteger(input);
        }

        public Priority priority() {
            return Priority.NORMAL;
        }
    }

    static class BigDecimalArgument<S extends Source> extends NumberArgument<S, java.math.BigDecimal> {

        protected BigDecimalArgument() {
            super();
        }

        @Override
        public String display() {
            return "big decimal";
        }

        @Override
        public java.math.BigDecimal parse(String input) throws NumberFormatException {
            return new java.math.BigDecimal(input);
        }

        public Priority priority() {
            return Priority.HIGH.plus(1);
        }
    }
}
