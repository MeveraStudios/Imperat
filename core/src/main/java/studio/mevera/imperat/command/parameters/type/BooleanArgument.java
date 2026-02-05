package studio.mevera.imperat.command.parameters.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.parse.InvalidBooleanException;

import java.util.Locale;
import java.util.Map;

public final class BooleanArgument<S extends Source> extends ArgumentType<S, Boolean> {

    private final static Map<String, Boolean> VARIANTS = Map.of(
            "t", true, "f", false,
            "yes", true, "no", false,
            "y", true, "n", false,
            "on", true, "off", false,
            "enabled", true, "disabled", false
    );

    private boolean allowVariants = false;

    BooleanArgument() {
        super();
        addStaticSuggestions("true", "false");
    }

    @Override
    public @Nullable Boolean resolve(@NotNull ExecutionContext<S> context, @NotNull Cursor<S> cursor, @NotNull String correspondingInput)
            throws
            CommandException {

        var raw = cursor.currentRaw().orElse(null);
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
    public boolean matchesInput(int rawPosition, Context<S> context, Argument<S> parameter) {
        String currentInput = context.arguments().get(rawPosition);
        if (currentInput == null) {
            return false;
        }

        if (!allowVariants && (currentInput.equalsIgnoreCase("true") || currentInput.equalsIgnoreCase("false"))) {
            return true;
        } else if (allowVariants) {
            return VARIANTS.get(currentInput.toLowerCase(Locale.ENGLISH)) != null;
        }

        return Boolean.parseBoolean(currentInput);
    }

    public BooleanArgument<S> setAllowVariants(boolean allowVariants) {
        this.allowVariants = allowVariants;
        if (allowVariants) {
            suggestions.addAll(VARIANTS.keySet());
        } else {
            suggestions.removeAll(VARIANTS.keySet());
        }
        return this;
    }

    public BooleanArgument<S> allowVariants() {
        return setAllowVariants(true);
    }
}
