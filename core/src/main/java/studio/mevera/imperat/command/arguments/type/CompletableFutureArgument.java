package studio.mevera.imperat.command.arguments.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.DefaultValueProvider;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.providers.SuggestionProvider;
import studio.mevera.imperat.util.TypeWrap;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class CompletableFutureArgument<S extends CommandSource, T> extends ArgumentType<S, CompletableFuture<T>> {

    private final ArgumentType<S, T> typeResolver;

    public CompletableFutureArgument(TypeWrap<CompletableFuture<T>> typeWrap, ArgumentType<S, T> typeResolver) {
        super(typeWrap.getType());
        this.typeResolver = typeResolver;
    }

    @Override
    public CompletableFuture<T> parse(@NotNull CommandContext<S> context, @NotNull Argument<S> argument, @NotNull Cursor<S> cursor) {
        if (typeResolver == null) {
            throw new IllegalStateException("No type parameter for type '" + type.getTypeName() + "'");
        }
        // Drain ALL tokens belonging to this argument on the calling thread
        // (cursors aren't thread-safe), then hand the inner parse a fresh
        // multi-token cursor backed by the drained list on the async worker.
        //
        // Multi-arity inner types (e.g. SimpleArgumentType(N) with N > 1, like
        // a 3-token coordinate triple) require all N tokens to be drained up
        // front; draining only one would leave the inner type's cursor short
        // and silently steal tokens from downstream arguments.
        List<String> drained = drainTokens(argument, cursor);
        if (drained.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                return typeResolver.parse(context, argument, Cursor.of(context, drained));
            } catch (Exception ex) {
                context.imperatConfig().handleExecutionError(ex, context, CompletableFutureArgument.class, "parse");
                return null;
            }
        });
    }

    /**
     * Drains the tokens this future's inner type expects, on the calling
     * thread. Returns an empty list when input is exhausted (which the parse
     * path translates into a {@code completedFuture(null)} — same observable
     * shape as the legacy single-token implementation).
     */
    private List<String> drainTokens(Argument<S> argument, Cursor<S> cursor) {
        if (typeResolver.isGreedy(argument)) {
            List<String> all = new ArrayList<>(cursor.remaining());
            while (cursor.hasNext()) {
                all.add(cursor.next());
            }
            return all;
        }
        int n = Math.max(1, typeResolver.getNumberOfParametersToConsume(argument));
        if (cursor.remaining() < n) {
            // Tree pre-allocates exactly N tokens via getNumberOfParametersToConsume,
            // so this branch is essentially defensive — but mirrors the
            // legacy "no input → null future" semantic for safety.
            return List.of();
        }
        List<String> tokens = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            tokens.add(cursor.next());
        }
        return tokens;
    }

    @Override
    public SuggestionProvider<S> getSuggestionProvider() {
        return typeResolver.getSuggestionProvider();
    }

    @Override
    public DefaultValueProvider getDefaultValueProvider() {
        return typeResolver.getDefaultValueProvider();
    }

    @Override
    public boolean isGreedy(Argument<S> parameter) {
        return typeResolver.isGreedy(parameter);
    }

    @Override
    public int getNumberOfParametersToConsume(Argument<S> argument) {
        return typeResolver.getNumberOfParametersToConsume(argument);
    }

}
