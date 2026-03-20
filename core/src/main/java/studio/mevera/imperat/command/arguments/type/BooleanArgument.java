package studio.mevera.imperat.command.arguments.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.ArgumentParseException;
import studio.mevera.imperat.responses.ResponseKey;
import studio.mevera.imperat.util.priority.Priority;

import java.util.Map;

public final class BooleanArgument<S extends CommandSource> extends ArgumentType<S, Boolean> {

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
    public Boolean parse(@NotNull CommandContext<S> context, @NotNull String input) throws ArgumentParseException {
        if (input.equalsIgnoreCase("true") || input.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(input);
        }
        if (allowVariants) {
            Boolean value = VARIANTS.get(input.toLowerCase());
            if (value != null) {
                return value;
            }
        }
        throw new ArgumentParseException(ResponseKey.INVALID_BOOLEAN, input);
    }

    @Override
    public Boolean parse(@NotNull ExecutionContext<S> context, @NotNull Cursor<S> cursor) throws ArgumentParseException {
        String input = cursor.currentRawIfPresent();
        if (input == null) {
            throw new IllegalArgumentException("No input available at cursor position");
        }
        return parse(context, input);
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

    @Override
    public @NotNull Priority getPriority() {
        return Priority.NORMAL.plus(1);
    }
}
