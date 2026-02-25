package studio.mevera.imperat.annotations.parameters;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.command.parameters.InputParameter;
import studio.mevera.imperat.command.parameters.NumericParameter;
import studio.mevera.imperat.command.parameters.NumericRange;
import studio.mevera.imperat.command.parameters.validator.RangeValidator;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.providers.SuggestionProvider;

public final class NumericArgumentDecorator<S extends Source> extends InputParameter<S> implements NumericParameter<S> {

    private final Argument<S> parameter;
    private final NumericRange range;

    NumericArgumentDecorator(Argument<S> parameter, NumericRange range) {
        super(
                parameter.getName(), parameter.type(), parameter.getPermissionsData(),
                parameter.getDescription(), parameter.isOptional(), parameter.isFlag(),
                parameter.isFlag(), parameter.getDefaultValueSupplier(),
                loadSuggestionResolver(parameter, range)
        );
        this.parameter = parameter;
        this.range = range;
        for (var validator : parameter.getValidators()) {
            this.addValidator(validator);
        }
        this.addValidator(new RangeValidator<>());
    }


    public static <S extends Source> NumericArgumentDecorator<S> decorate(@NotNull Argument<S> parameter, @NotNull NumericRange range) {
        return new NumericArgumentDecorator<>(parameter, range);
    }

    private static <S extends Source> SuggestionProvider<S> loadSuggestionResolver(Argument<S> parameter, NumericRange range) {
        var def = parameter.getSuggestionResolver();
        if (parameter.getSuggestionResolver() != null || (range.getMin() == Double.MIN_VALUE && range.getMax() == Double.MAX_VALUE)) {
            return def;
        }

        String suggestion;
        if (range.getMin() != Double.MIN_VALUE && range.getMax() == Double.MAX_VALUE) {
            suggestion = range.getMin() + "";
        } else if (range.getMin() == Double.MIN_VALUE) {
            suggestion = range.getMax() + "";
        } else {
            suggestion = range.getMin() + "-" + range.getMax();
        }
        return SuggestionProvider.staticSuggestions(suggestion);
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

}
