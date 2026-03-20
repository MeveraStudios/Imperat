package studio.mevera.imperat.type;

import com.hypixel.hytale.server.core.command.system.ParseResult;
import com.hypixel.hytale.server.core.command.system.suggestion.SuggestionResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.HytaleCommandSource;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.ArgumentParseException;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.providers.SuggestionProvider;
import studio.mevera.imperat.responses.ResponseKey;

public class HytaleArgumentType<T> extends ArgumentType<HytaleCommandSource, T> {

    private final com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType<T> hytaleArgType;
    private final ExceptionProvider exceptionProvider;
    private final SuggestionProvider<HytaleCommandSource> suggestionProvider;

    public HytaleArgumentType(Class<T> type, com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType<T> hytaleArgType, ExceptionProvider provider) {
        super(type);
        this.hytaleArgType = hytaleArgType;
        this.exceptionProvider = provider;
        this.suggestionProvider = (ctx, Argument) -> {
            SuggestionResult result = new SuggestionResult();
            hytaleArgType.suggest(ctx.source().origin(), ctx.getArgToComplete().value(), ctx.arguments().size(), result);
            return result.getSuggestions();
        };
    }

    public HytaleArgumentType(Data<T> data) {
        this(data.type, data.argumentType, data.provider);
    }


    @Override
    public @NotNull T parse(@NotNull CommandContext<HytaleCommandSource> context, @NotNull String input) throws CommandException {
        // This type is not intended for direct string parsing; must use cursor-based parsing.
        throw new UnsupportedOperationException("HytaleArgumentType does not support parse(context, String); use parse(context, Cursor) instead.");
    }

    @Override
    public @Nullable T parse(@NotNull ExecutionContext<HytaleCommandSource> context, @NotNull Cursor<HytaleCommandSource> cursor)
            throws CommandException {
        String[] rawInput = context.arguments().toArray(String[]::new);
        final ParseResult parseResult = new ParseResult();
        T parsedArg = hytaleArgType.parse(rawInput, parseResult);
        if (parseResult.failed()) {
            // Use the current raw input for error context if available
            String input = cursor.currentRawIfPresent();
            throw exceptionProvider.fetch(input != null ? input : "<unknown>");
        } else {
            // Success, skip the same amount
            int numberOfArgs = hytaleArgType.getNumberOfParameters();
            for (int i = 1; i < numberOfArgs; i++) {
                cursor.skipRaw();
            }
            return parsedArg;
        }
    }

    @Override
    public int getNumberOfParametersToConsume(Argument<HytaleCommandSource> argument) {
        return hytaleArgType.getNumberOfParameters();
    }

    @Override
    public SuggestionProvider<HytaleCommandSource> getSuggestionProvider() {
        return suggestionProvider;
    }

    public com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType<T> getHytaleArgType() {
        return hytaleArgType;
    }

    @Override
    public boolean isGreedy(Argument<HytaleCommandSource> parameter) {
        return hytaleArgType.isListArgument();
    }

    @FunctionalInterface
    public interface ExceptionProvider {

        ExceptionProvider DEFAULT = (in) -> new ArgumentParseException(ResponseKey.INVALID_UUID, in);

        CommandException fetch(String input);
    }

    /**
     * Wrapper for ExceptionProvider that uses a ResponseKey.
     * This allows centralized error message management through the response system.
     */
    public static class ResponseKeyExceptionProvider implements ExceptionProvider {

        private final ResponseKey responseKey;

        public ResponseKeyExceptionProvider(ResponseKey responseKey) {
            this.responseKey = responseKey;
        }

        @Override
        public CommandException fetch(String input) {
            return new ArgumentParseException(responseKey, input);
        }
    }

    public record Data<T>(Class<T> type, com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType<T> argumentType, ExceptionProvider provider) {

    }
}
