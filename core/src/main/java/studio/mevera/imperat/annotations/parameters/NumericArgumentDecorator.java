package studio.mevera.imperat.annotations.parameters;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.InputArgument;
import studio.mevera.imperat.command.arguments.NumericArgument;
import studio.mevera.imperat.command.arguments.NumericRange;
import studio.mevera.imperat.command.arguments.validator.RangeValidator;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.providers.SuggestionProvider;

public final class NumericArgumentDecorator<S extends CommandSource> extends InputArgument<S> implements NumericArgument<S> {

    private final Argument<S> parameter;
    private final @NotNull NumericRange range;

    NumericArgumentDecorator(Argument<S> parameter, @NotNull NumericRange range) {
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


    public static <S extends CommandSource> NumericArgumentDecorator<S> decorate(@NotNull Argument<S> parameter, @NotNull NumericRange range) {
        return new NumericArgumentDecorator<>(parameter, range);
    }

    private static <S extends CommandSource> SuggestionProvider<S> loadSuggestionResolver(Argument<S> parameter, NumericRange range) {
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
     * @return The actual range of the numeric argument.
     * returns the range for the numeric arg, may be empty, but the instance is never null!
     */
    @Override
    public @NotNull NumericRange getRange() {
        return range;
    }

}
