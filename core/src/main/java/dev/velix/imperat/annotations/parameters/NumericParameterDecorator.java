package dev.velix.imperat.annotations.parameters;

import dev.velix.imperat.command.parameters.CommandParameter;
import dev.velix.imperat.command.parameters.InputParameter;
import dev.velix.imperat.command.parameters.NumericParameter;
import dev.velix.imperat.command.parameters.NumericRange;
import dev.velix.imperat.context.Source;
import dev.velix.imperat.resolvers.SuggestionResolver;
import org.jetbrains.annotations.*;

public final class NumericParameterDecorator<S extends Source> extends InputParameter<S> implements NumericParameter<S> {

    private final CommandParameter<S> parameter;
    private final NumericRange range;

    NumericParameterDecorator(CommandParameter<S> parameter, NumericRange range) {
        super(
            parameter.name(), parameter.type(), parameter.permission(),
            parameter.description(), parameter.isOptional(), parameter.isFlag(),
            parameter.isFlag(), parameter.getDefaultValueSupplier(),
            loadSuggestionResolver(parameter, range)
        );
        this.parameter = parameter;
        this.range = range;
    }


    public static <S extends Source> NumericParameterDecorator<S> decorate(@NotNull CommandParameter<S> parameter, @NotNull NumericRange range) {
        return new NumericParameterDecorator<>(parameter, range);
    }

    /**
     * Formats the usage parameter
     * using the command
     *
     * @return the formatted parameter
     */
    @Override
    public String format() {
        return parameter.format();
    }

    /**
     * @return The actual range of the numeric parameter
     * returns null if no range is specified!
     */
    @Override
    public @Nullable NumericRange getRange() {
        return range;
    }

    private static <S extends Source> SuggestionResolver<S> loadSuggestionResolver(CommandParameter<S> parameter, NumericRange range) {
        var def = parameter.getSuggestionResolver();
        if(parameter.getSuggestionResolver() != null|| (range.getMin() == Double.MIN_VALUE && range.getMax() == Double.MAX_VALUE)) {
            return def;
        }

        String suggestion;
        if(range.getMin() != Double.MIN_VALUE && range.getMax() == Double.MAX_VALUE) {
            suggestion = range.getMin() + "";
        }
        else if(range.getMin() == Double.MIN_VALUE) {
            suggestion = range.getMax() + "";
        }else {
            suggestion = range.getMin() + "-" + range.getMax();
        }
        return SuggestionResolver.plain(suggestion);
    }

}
