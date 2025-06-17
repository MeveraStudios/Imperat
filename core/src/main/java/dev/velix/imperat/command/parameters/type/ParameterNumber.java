package dev.velix.imperat.command.parameters.type;

import dev.velix.imperat.command.parameters.CommandParameter;
import dev.velix.imperat.context.ExecutionContext;
import dev.velix.imperat.context.Source;
import dev.velix.imperat.context.internal.CommandInputStream;
import dev.velix.imperat.exception.ImperatException;
import dev.velix.imperat.exception.parse.InvalidNumberFormatException;
import dev.velix.imperat.util.TypeUtility;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class ParameterNumber<S extends Source, N extends Number> extends BaseParameterType<S, N> {

    protected ParameterNumber() {
        super();
    }

    @SuppressWarnings("unchecked")
    static <S extends Source, N extends Number> ParameterNumber<S, N> from(Class<N> numType) {
        if (TypeUtility.matches(numType, Integer.class)) {
            return (ParameterNumber<S, N>) new ParameterInt<>();
        } else if (TypeUtility.matches(numType, Long.class)) {
            return (ParameterNumber<S, N>) new ParameterLong<>();
        } else if (TypeUtility.matches(numType, Float.class)) {
            return (ParameterNumber<S, N>) new ParameterFloat<>();
        } else if (TypeUtility.matches(numType, Double.class)) {
            return (ParameterNumber<S, N>) new ParameterDouble<>();
        } else {
            throw new IllegalArgumentException("Unsupported number type: " + numType.getTypeName());
        }
    }

    @Override
    public @Nullable N resolve(@NotNull ExecutionContext<S> context, @NotNull CommandInputStream<S> commandInputStream, String input) throws ImperatException {
        try {
            return parse(input);
        } catch (NumberFormatException ex) {
            throw new InvalidNumberFormatException(input,ex, display(), this.wrappedType());
        }
    }

    @Override
    public boolean matchesInput(String input, CommandParameter<S> parameter) {
        try {
            parse(input);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public abstract String display();

    public abstract N parse(String input) throws NumberFormatException;

    static class ParameterInt<S extends Source> extends ParameterNumber<S, Integer> {

        protected ParameterInt() {
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
    }

    static class ParameterFloat<S extends Source> extends ParameterNumber<S, Float> {

        protected ParameterFloat() {
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
    }

    static class ParameterLong<S extends Source> extends ParameterNumber<S, Long> {

        protected ParameterLong() {
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
    }

    static class ParameterDouble<S extends Source> extends ParameterNumber<S, Double> {

        protected ParameterDouble() {
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

    }
}
