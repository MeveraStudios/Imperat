package studio.mevera.imperat.annotations.parameters;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.InputArgument;
import studio.mevera.imperat.command.arguments.NumericArgument;
import studio.mevera.imperat.command.arguments.NumericRange;
import studio.mevera.imperat.command.arguments.type.NumberArgument;
import studio.mevera.imperat.command.arguments.validator.RangeValidator;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.providers.SuggestionProvider;

import java.util.ArrayList;
import java.util.List;

public final class NumericArgumentDecorator<S extends CommandSource> extends InputArgument<S> implements NumericArgument<S> {

    private final Argument<S> parameter;
    private final @NotNull NumericRange range;

    NumericArgumentDecorator(Argument<S> argument, @NotNull NumericRange range) {
        super(
                argument.getName(), argument.type(), argument.getPermissionsData(),
                argument.getDescription(), argument.isOptional(), argument.isFlag(),
                argument.isFlag(), argument.getDefaultValueSupplier(),
                loadSuggestionResolver(argument, range)
        );
        this.parameter = argument;
        this.range = range;
        for (var validator : argument.getValidators()) {
            this.addValidator(validator);
        }
        this.addValidator(new RangeValidator<>());
    }


    public static <S extends CommandSource> NumericArgumentDecorator<S> decorate(@NotNull Argument<S> parameter, @NotNull NumericRange range) {
        return new NumericArgumentDecorator<>(parameter, range);
    }

    private static <N extends Number, S extends CommandSource> SuggestionProvider<S> loadSuggestionResolver(
            Argument<S> arg,
            NumericRange range
    ) {
        var def = arg.getSuggestionResolver();
        if (arg.getSuggestionResolver() != null || range.isEmpty()) {
            return def;
        }
        NumberArgument<S, N> numberArgumentType = (NumberArgument<S, N>) arg.type();

        if (range.hasMin() && range.hasMax()) {
            List<String> suggestions = new ArrayList<>(3);
            double low = range.getMin();
            double high = range.getMax();
            double mid = (low + high) / 2;

            suggestions.add(String.valueOf(numberArgumentType.cast(low)));
            suggestions.add(String.valueOf(numberArgumentType.cast(mid)));
            suggestions.add(String.valueOf(numberArgumentType.cast(mid)));
            return SuggestionProvider.staticSuggestions(suggestions);
        } else if (range.hasMin()) {
            return SuggestionProvider.staticSuggestions(String.valueOf(numberArgumentType.cast(range.getMin())));
        }

        return null;
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
