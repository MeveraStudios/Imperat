package studio.mevera.imperat.backend.modern.argument;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.BukkitCommandSource;
import studio.mevera.imperat.command.suggestions.CompletionArg;
import studio.mevera.imperat.context.SuggestionContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Bridge between Imperat's suggestion machinery and Brigadier's
 * {@link ArgumentType#listSuggestions} API.
 *
 * <p>Imperat-side suggestion providers receive a
 * {@link SuggestionContext} + {@link studio.mevera.imperat.command.arguments.Argument
 * Argument}. Brigadier-native types expose suggestions via
 * {@code listSuggestions(CommandContext, SuggestionsBuilder)} — a
 * different shape that's gated on Brigadier's parse pipeline producing
 * the {@link CommandContext}.</p>
 *
 * <p>This bridge synthesises a minimal Brigadier {@code CommandContext}
 * (backed by a long-lived empty {@link CommandDispatcher}) and invokes
 * the native type's {@code listSuggestions}, then folds the resulting
 * {@link Suggestions} back into the {@code List<String>} shape Imperat's
 * suggester consumes. Server-side the {@link CommandSourceStack} comes
 * from {@link BukkitCommandSource#stack()} so selector permission gates
 * (entity selectors require op level 2) evaluate against the real
 * sender.</p>
 *
 * <p>Use this when an Imperat-side {@link
 * studio.mevera.imperat.command.arguments.type.ArgumentType ArgumentType}
 * wants to delegate completions to a Paper-native type (e.g.
 * {@code ArgumentTypes.entities()} for selector autocomplete) without
 * giving up its own server-side parser.</p>
 *
 * @since 4.0.0
 */
public final class PaperBrigadierSuggestionBridge {

    /**
     * Synthetic dispatcher held purely to satisfy Brigadier's
     * {@link CommandContextBuilder} constructor — its tree is empty and
     * never used. Reused across calls (no thread-safety risk: the bridge
     * only reads {@link CommandDispatcher#getRoot()}, which is final).
     */
    private static final CommandDispatcher<CommandSourceStack> SYNTHETIC_DISPATCHER = new CommandDispatcher<>();

    private PaperBrigadierSuggestionBridge() {
    }

    /**
     * Bridges a Paper-native {@link ArgumentType}'s suggestions into
     * Imperat's flat {@code List<String>} shape, using {@code imperatCtx}
     * to derive the input buffer + completion cursor + source stack.
     *
     * <p>Returns an empty list (never null) on any failure path so callers
     * can fold this into the standard suggestion-provider chain without
     * extra null-handling.</p>
     */
    public static @NotNull List<String> bridge(
            @NotNull SuggestionContext<BukkitCommandSource> imperatCtx,
            @NotNull ArgumentType<?> nativeType
    ) {
        Object stackObj = imperatCtx.source().stack();
        if (!(stackObj instanceof CommandSourceStack stack)) {
            // Plain backend / non-modern-Paper environment — selector
            // resolvers need a real CommandSourceStack to evaluate
            // permissions; bail out so the suggester falls through to
            // whatever provider the framework has registered.
            return List.of();
        }

        String input = buildInput(imperatCtx);
        int start = resolveStart(input, imperatCtx.getArgToComplete());
        SuggestionsBuilder builder = new SuggestionsBuilder(input, start);

        CommandContext<CommandSourceStack> brigCtx = synthesiseContext(stack, input);

        try {
            Suggestions suggestions = nativeType.listSuggestions(brigCtx, builder).join();
            List<String> out = new ArrayList<>(suggestions.getList().size());
            for (Suggestion suggestion : suggestions.getList()) {
                String text = suggestion.getText();
                if (text != null && !text.isEmpty()) {
                    out.add(text);
                }
            }
            return out;
        } catch (Throwable ex) {
            // Native type may throw on malformed buffer or permission
            // denial — degrade silently, callers can still serve their
            // own non-native suggestions.
            return List.of();
        }
    }

    private static String buildInput(SuggestionContext<BukkitCommandSource> ctx) {
        String raw = ctx.arguments().getOriginalRaw();
        return raw == null ? "" : raw;
    }

    private static int resolveStart(String input, CompletionArg arg) {
        if (arg == null || arg.isEmpty()) {
            return input.length();
        }
        String value = arg.value();
        if (value == null || value.isEmpty()) {
            return input.length();
        }
        return Math.max(0, input.length() - value.length());
    }

    private static CommandContext<CommandSourceStack> synthesiseContext(
            CommandSourceStack stack, String input
    ) {
        CommandContextBuilder<CommandSourceStack> builder = new CommandContextBuilder<>(
                SYNTHETIC_DISPATCHER, stack, SYNTHETIC_DISPATCHER.getRoot(), 0
        );
        return builder.build(input);
    }
}
