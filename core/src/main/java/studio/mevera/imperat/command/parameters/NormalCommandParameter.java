package studio.mevera.imperat.command.parameters;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Description;
import studio.mevera.imperat.command.parameters.type.ParameterType;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.resolvers.SuggestionResolver;
import studio.mevera.imperat.util.StringUtils;

class NormalCommandParameter<S extends Source> extends InputParameter<S> {

    NormalCommandParameter(String name,
                           ParameterType<S, ?> type,
                           @Nullable String permission,
                           Description description,
                           boolean optional,
                           boolean greedy,
                           @NotNull OptionalValueSupplier valueSupplier,
                           @Nullable SuggestionResolver<S> suggestionResolver) {
        super(
            name, type, permission, description, optional,
            false, greedy, valueSupplier, suggestionResolver
        );
    }

    /**
     * Formats the usage parameter
     *
     * @return the formatted parameter
     */
    @Override
    public String format() {
        var content = name();
        if (isGreedy())
            content += "...";
        return StringUtils.normalizedParameterFormatting(content, isOptional());
    }
}
