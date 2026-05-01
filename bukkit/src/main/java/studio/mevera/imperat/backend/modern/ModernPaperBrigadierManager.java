package studio.mevera.imperat.backend.modern;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.BaseBrigadierManager;
import studio.mevera.imperat.BukkitCommandSource;
import studio.mevera.imperat.BukkitImperat;
import studio.mevera.imperat.backend.modern.argument.PaperNativeArgumentType;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.FlagArgument;

import java.util.ArrayList;
import java.util.List;

/**
 * Modern-Paper Brigadier registrar. Extends the framework's
 * {@link BaseBrigadierManager} (projection-driven) to produce a
 * Brigadier {@code LiteralCommandNode<CommandSourceStack>} per Imperat
 * command, then hands it to Paper's {@link Commands} registrar through
 * the {@code COMMANDS} lifecycle event.
 *
 * <p>Argument-type lookup goes through the unified
 * {@link PaperNativeArgumentType} SPI — covers both the
 * {@code PaperBukkitArgumentType} wrappers ({@code World},
 * {@code GameMode}, {@code ItemStack}, etc.) and Imperat-side parsers
 * with native rendering ({@code PaperTargetSelectorArgument}). Imperat
 * types without a native counterpart fall back to plain string.</p>
 *
 * @since 4.0.0 (Paper module)
 */
public final class ModernPaperBrigadierManager extends BaseBrigadierManager<BukkitCommandSource> {

    private final BukkitImperat bukkitImperat;

    public ModernPaperBrigadierManager(@NotNull BukkitImperat imperat) {
        super(imperat);
        this.bukkitImperat = imperat;
    }

    @Override
    public BukkitCommandSource wrapCommandSource(Object commandSource) {
        return bukkitImperat.wrapSender(commandSource);
    }

    @Override
    public @NotNull com.mojang.brigadier.arguments.ArgumentType<?> getArgumentType(
            @NotNull Argument<BukkitCommandSource> imperatArgument
    ) {
        com.mojang.brigadier.arguments.ArgumentType<?> nativeType = paperNativeOf(imperatArgument.type());
        return nativeType != null ? nativeType : getStringArgType(imperatArgument);
    }

    @Override
    protected com.mojang.brigadier.arguments.@NotNull ArgumentType<?> getFlagValueArgumentType(
            @NotNull FlagArgument<BukkitCommandSource> flag
    ) {
        var inputType = flag.flagData().inputType();
        if (inputType == null) {
            return super.getFlagValueArgumentType(flag);
        }
        com.mojang.brigadier.arguments.ArgumentType<?> nativeType = paperNativeOf(inputType);
        return nativeType != null ? nativeType : super.getFlagValueArgumentType(flag);
    }

    /**
     * Routes positional-arg tab completion to the Paper-native type's
     * {@code listSuggestions} when the Imperat-side type implements
     * {@link PaperNativeArgumentType}. Without this override, the base
     * class would emit Imperat-tree-derived completions; the native
     * parser already produces richer client-aware ones (selector char
     * menu, filter keys, dimension list, etc.).
     */
    @Override
    protected @NotNull <BS> SuggestionProvider<BS> createSuggestionProvider(
            Command<BukkitCommandSource> command,
            Argument<BukkitCommandSource> parameter
    ) {
        com.mojang.brigadier.arguments.ArgumentType<?> nativeType = paperNativeOf(parameter.type());
        if (nativeType != null) {
            @SuppressWarnings({"rawtypes", "unchecked"})
            com.mojang.brigadier.arguments.ArgumentType raw = nativeType;
            return (ctx, builder) -> raw.listSuggestions(ctx, builder);
        }
        return super.createSuggestionProvider(command, parameter);
    }

    /**
     * Flag-value variant of {@link #createSuggestionProvider} — same
     * native-delegation rule, applied to the flag's input type.
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected <BS> @Nullable SuggestionProvider<BS> createNativeFlagValueSuggester(
            @NotNull FlagArgument<BukkitCommandSource> flag
    ) {
        var inputType = flag.flagData().inputType();
        if (inputType == null) {
            return null;
        }
        com.mojang.brigadier.arguments.ArgumentType<?> nativeType = paperNativeOf(inputType);
        if (nativeType == null) {
            return null;
        }
        com.mojang.brigadier.arguments.ArgumentType raw = nativeType;
        return (ctx, builder) -> raw.listSuggestions(ctx, builder);
    }

    /**
     * Modern Paper opts out of the inline-flag catch-all sibling.
     *
     * <p>Two failed paths walked first:
     * <ul>
     *   <li>Raw {@link studio.mevera.imperat.InlineFlagArgumentType}: rejected by Paper's
     *       {@code Commands} registrar with
     *       "Custom unknown argument type was passed, should be wrapped
     *       inside a CustomArgumentType".</li>
     *   <li>Wrapped via {@code CustomArgumentType} with
     *       {@code greedyString} as native: registers, but the greedy
     *       native sent to the client confuses Brigadier's client-side
     *       tree-walk for completions — sibling nodes never collect
     *       suggestions because the client thinks {@code <flag>}
     *       consumes the rest of input.</li>
     * </ul></p>
     *
     * <p>No vanilla native type accepts {@code =} in single-token form
     * AND leaves siblings reachable for client-side completion. Skip the
     * node here. Inline {@code -flag=value} renders red on modern Paper,
     * but completions still flow via the
     * {@link BaseBrigadierManager}-level positional-suggester wrapper
     * (delegates to the Imperat tree), and execution succeeds via the
     * {@code UnknownCommandEvent} fallback in {@code BukkitImperat}.</p>
     */
    @Override
    protected com.mojang.brigadier.arguments.@Nullable ArgumentType<?> inlineFlagArgumentType() {
        return null;
    }

    /**
     * Builds the Brigadier node tree for {@code command} and registers it
     * with Paper's {@link Commands} registrar (captured during the
     * {@code COMMANDS} lifecycle event).
     */
    public void register(@NotNull Commands registrar, @NotNull Command<BukkitCommandSource> command) {
        LiteralCommandNode<CommandSourceStack> node = this.parseCommandIntoNode(command);
        String description = command.getDescription() != null
                                     ? command.getDescription().getValueOrElse("")
                                     : "";
        List<String> aliases = new ArrayList<>(command.aliases());
        registrar.register(node, description.isEmpty() ? null : description, aliases);
    }

    /**
     * Returns the Paper Brigadier-native type backing {@code imperatType},
     * or {@code null} if the type doesn't expose one. Single source of
     * truth used by every native-aware override on this class:
     * {@link #getArgumentType(Argument)}, {@link #getFlagValueArgumentType(FlagArgument)},
     * {@link #createSuggestionProvider(Command, Argument)}, and
     * {@link #createNativeFlagValueSuggester(FlagArgument)}.
     */
    private com.mojang.brigadier.arguments.@Nullable ArgumentType<?> paperNativeOf(
            studio.mevera.imperat.command.arguments.type.ArgumentType<BukkitCommandSource, ?> imperatType
    ) {
        return imperatType instanceof PaperNativeArgumentType paperNative
                       ? paperNative.nativeType()
                       : null;
    }
}
