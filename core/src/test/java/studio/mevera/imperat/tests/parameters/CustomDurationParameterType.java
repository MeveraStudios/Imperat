package studio.mevera.imperat.tests.parameters;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.parameters.type.BaseParameterType;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.exception.ImperatException;
import studio.mevera.imperat.exception.SourceException;
import studio.mevera.imperat.resolvers.SuggestionResolver;

public class CustomDurationParameterType<S extends Source> extends BaseParameterType<S, CustomDuration> {

    private final SuggestionResolver<S> resolver = SuggestionResolver.staticSuggestions(
            "permanent", "30d", "1y", "5y"
    );

    public CustomDurationParameterType() {
        super();
    }

    @Override
    public @Nullable CustomDuration resolve(
            @NotNull ExecutionContext<S> context,
            @NotNull CommandInputStream<S> cis,
            @NotNull String input
    ) throws ImperatException {
        final long ms = TimeUtil.convertDurationToMs(input);
        if (ms == 0) {
            throw new SourceException("Bad duration input '" + input + "'");
        }
        return new CustomDuration(ms);
    }

    @Override
    public SuggestionResolver<S> getSuggestionResolver() {
        return resolver;
    }

}