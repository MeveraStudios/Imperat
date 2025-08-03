package studio.mevera.imperat.command.parameters.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.exception.ImperatException;
import studio.mevera.imperat.exception.parse.InvalidBooleanException;

import java.util.Locale;
import java.util.Map;

public final class ParameterBoolean<S extends Source> extends BaseParameterType<S, Boolean> {

    private final static Map<String, Boolean> VARIANTS = Map.of(
        "t", true, "f", false,
        "yes", true, "no", false,
        "y", true, "n", false,
        "on", true, "off", false,
        "enabled", true, "disabled", false
    );

    private boolean allowVariants = false;

    ParameterBoolean() {
        super();
        withSuggestions("true", "false");
    }

    @Override
    public @Nullable Boolean resolve(@NotNull ExecutionContext<S> context, @NotNull CommandInputStream<S> commandInputStream, @NotNull String input) throws ImperatException {

        var raw = commandInputStream.currentRaw().orElse(null);
        assert raw != null;

        if (raw.equalsIgnoreCase("true") || raw.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(raw);
        }

        if (allowVariants) {
            return VARIANTS.get(raw.toLowerCase());
        } else {
            throw new InvalidBooleanException(raw);
        }
    }

    @Override
    public boolean matchesInput(String input, CommandParameter<S> parameter) {

        if (!allowVariants && (input.equalsIgnoreCase("true") || input.equalsIgnoreCase("false")))
            return true;
        else if (allowVariants) {
            return VARIANTS.get(input.toLowerCase(Locale.ENGLISH)) != null;
        }

        return Boolean.parseBoolean(input);
    }

    public ParameterBoolean<S> setAllowVariants(boolean allowVariants) {
        this.allowVariants = allowVariants;
        if (allowVariants) {
            suggestions.addAll(VARIANTS.keySet());
        } else {
            suggestions.removeAll(VARIANTS.keySet());
        }
        return this;
    }

    public ParameterBoolean<S> allowVariants() {
        return setAllowVariants(true);
    }
}
