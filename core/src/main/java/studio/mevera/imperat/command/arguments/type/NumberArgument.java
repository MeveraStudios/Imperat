package studio.mevera.imperat.command.arguments.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.exception.ArgumentParseException;
import studio.mevera.imperat.exception.ResponseException;
import studio.mevera.imperat.providers.SuggestionProvider;
import studio.mevera.imperat.responses.ResponseKey;
import studio.mevera.imperat.util.TypeUtility;
import studio.mevera.imperat.util.priority.Priority;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;

public abstract class NumberArgument<S extends CommandSource, N extends Number> extends ArgumentType<S, N> {
    protected NumberArgument() {
        super();
    }

    @SuppressWarnings("unchecked")
    static <S extends CommandSource, N extends Number> NumberArgument<S, N> from(Class<N> numType) {
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
    public abstract N parse(@NotNull CommandContext<S> context, @NotNull Argument<S> argument, @NotNull String input) throws ResponseException;


    public abstract String display();

    public abstract N cast(double value) throws ClassCastException;

    static class IntArgument<S extends CommandSource> extends NumberArgument<S, Integer> {

        protected IntArgument() {
            super();
        }
        @Override
        public String display() {
            return "integer";
        }

        @Override
        public Integer cast(double value) throws ClassCastException {
            return (int) value;
        }

        @Override
        public Integer parse(@NotNull CommandContext<S> context, @NotNull Argument<S> argument, @NotNull String input)
                throws ArgumentParseException, ResponseException {
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException ex) {
                throw new ArgumentParseException(ResponseKey.INVALID_NUMBER_FORMAT, input)
                              .withPlaceholder("number_type", display());
            }
        }
        @Override
        public @NotNull Priority getPriority() {
            return Priority.NORMAL;
        }

        @Override
        public SuggestionProvider<S> getSuggestionProvider() {
            return (ctx, arg) -> Collections.singletonList("0");
        }
    }


    static class FloatArgument<S extends CommandSource> extends NumberArgument<S, Float> {

        protected FloatArgument() {
            super();
        }
        @Override
        public String display() {
            return "float";
        }

        @Override public Float cast(double value) throws ClassCastException {
            return (float) value;
        }

        @Override
        public Float parse(@NotNull CommandContext<S> context, @NotNull Argument<S> argument, @NotNull String input) throws ResponseException {
            try {
                return Float.parseFloat(input);
            } catch (NumberFormatException ex) {
                throw new ArgumentParseException(ResponseKey.INVALID_NUMBER_FORMAT, input)
                              .withPlaceholder("number_type", display());
            }
        }

        @Override
        public @NotNull Priority getPriority() {
            return Priority.HIGH;
        }
    }


    static class LongArgument<S extends CommandSource> extends NumberArgument<S, Long> {

        protected LongArgument() {
            super();
        }
        @Override
        public String display() {
            return "long";
        }

        @Override
        public Long cast(double value) throws ClassCastException {
            return (long) value;
        }

        @Override
        public Long parse(@NotNull CommandContext<S> context, @NotNull Argument<S> argument, @NotNull String input) throws ResponseException {
            try {
                return Long.parseLong(input);
            } catch (NumberFormatException ex) {
                throw new ArgumentParseException(ResponseKey.INVALID_NUMBER_FORMAT, input)
                              .withPlaceholder("number_type", display());
            }
        }

        @Override
        public @NotNull Priority getPriority() {
            return Priority.NORMAL.plus(1);
        }
    }


    static class DoubleArgument<S extends CommandSource> extends NumberArgument<S, Double> {

        protected DoubleArgument() {
            super();
        }
        @Override
        public String display() {
            return "double";
        }

        @Override
        public Double cast(double value) throws ClassCastException {
            return value;
        }

        @Override
        public Double parse(@NotNull CommandContext<S> context, @NotNull Argument<S> argument, @NotNull String input) throws ResponseException {
            try {
                return Double.parseDouble(input);
            } catch (NumberFormatException ex) {
                throw new ArgumentParseException(ResponseKey.INVALID_NUMBER_FORMAT, input)
                              .withPlaceholder("number_type", display());
            }
        }

        @Override
        public @NotNull Priority getPriority() {
            return Priority.HIGH.plus(1);
        }
    }

    static class ByteArgument<S extends CommandSource> extends NumberArgument<S, Byte> {

        protected ByteArgument() {
            super();
        }
        @Override
        public String display() {
            return "byte";
        }

        @Override public Byte cast(double value) throws ClassCastException {
            return (byte) value;
        }

        @Override
        public Byte parse(@NotNull CommandContext<S> context, @NotNull Argument<S> argument, @NotNull String input)
                throws ArgumentParseException, ResponseException {
            try {
                return Byte.parseByte(input);
            } catch (NumberFormatException ex) {
                throw new ArgumentParseException(ResponseKey.INVALID_NUMBER_FORMAT, input)
                              .withPlaceholder("number_type", display());
            }
        }

        @Override
        public @NotNull Priority getPriority() {
            return Priority.NORMAL;
        }
    }


    static class ShortArgument<S extends CommandSource> extends NumberArgument<S, Short> {

        protected ShortArgument() {
            super();
        }
        @Override
        public String display() {
            return "short";
        }

        @Override public Short cast(double value) throws ClassCastException {
            return (short) value;
        }

        @Override
        public Short parse(@NotNull CommandContext<S> context, @NotNull Argument<S> argument, @NotNull String input)
                throws ArgumentParseException, ResponseException {
            try {
                return Short.parseShort(input);
            } catch (NumberFormatException ex) {
                throw new ArgumentParseException(ResponseKey.INVALID_NUMBER_FORMAT, input)
                              .withPlaceholder("number_type", display());
            }
        }

        @Override
        public @NotNull Priority getPriority() {
            return Priority.NORMAL;
        }
    }


    static class BigIntegerArgument<S extends CommandSource> extends NumberArgument<S, java.math.BigInteger> {

        protected BigIntegerArgument() {
            super();
        }
        @Override
        public String display() {
            return "big integer";
        }

        @Override public BigInteger cast(double value) throws ClassCastException {
            return BigInteger.valueOf((long) value);
        }

        @Override
        public java.math.BigInteger parse(@NotNull CommandContext<S> context, @NotNull Argument<S> argument, @NotNull String input)
                throws ArgumentParseException, ResponseException {
            try {
                return new java.math.BigInteger(input);
            } catch (NumberFormatException ex) {
                throw new ArgumentParseException(ResponseKey.INVALID_NUMBER_FORMAT, input)
                              .withPlaceholder("number_type", display());
            }
        }

        @Override
        public @NotNull Priority getPriority() {
            return Priority.NORMAL;
        }
    }


    static class BigDecimalArgument<S extends CommandSource> extends NumberArgument<S, java.math.BigDecimal> {

        protected BigDecimalArgument() {
            super();
        }
        @Override
        public String display() {
            return "big decimal";
        }

        @Override
        public BigDecimal cast(double value) throws ClassCastException {
            return BigDecimal.valueOf(value);
        }

        @Override
        public java.math.BigDecimal parse(@NotNull CommandContext<S> context, @NotNull Argument<S> argument, @NotNull String input)
                throws ResponseException {
            try {
                return new java.math.BigDecimal(input);
            } catch (NumberFormatException ex) {
                throw new ArgumentParseException(ResponseKey.INVALID_NUMBER_FORMAT, input)
                              .withPlaceholder("number_type", display());
            }
        }

        @Override
        public @NotNull Priority getPriority() {
            return Priority.HIGH.plus(1);
        }
    }
}
