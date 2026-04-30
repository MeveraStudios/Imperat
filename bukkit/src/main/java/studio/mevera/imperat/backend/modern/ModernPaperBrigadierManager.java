package studio.mevera.imperat.backend.modern;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.BaseBrigadierManager;
import studio.mevera.imperat.BukkitCommandSource;
import studio.mevera.imperat.BukkitImperat;
import studio.mevera.imperat.backend.modern.argument.PaperBukkitArgumentType;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.arguments.Argument;

import java.util.ArrayList;
import java.util.List;

/**
 * Modern-Paper Brigadier registrar. Extends the framework's
 * {@link BaseBrigadierManager} (projection-driven) to produce a
 * Brigadier {@code LiteralCommandNode<CommandSourceStack>} per Imperat
 * command, then hands it to Paper's {@link Commands} registrar through
 * the {@code COMMANDS} lifecycle event.
 *
 * <p>No NMS reflection, no Commodore — Paper's stable Brigadier API
 * does the heavy lifting. Argument-type lookup is performed against
 * {@link PaperBukkitArgumentType}; Imperat types without a Paper
 * Brigadier counterpart fall through to a plain string argument.</p>
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
            @NotNull Argument<BukkitCommandSource> imperatArgument) {
        var type = imperatArgument.type();

        if (type instanceof PaperBukkitArgumentType<?, ?> paperType) {
            return paperType.nativeType();
        }
        // Generic Paper-native bridge: any Imperat-side ArgumentType that
        // implements PaperNativeAware contributes its Brigadier-native
        // counterpart for client coloring + native autocomplete, while
        // keeping its custom server-side parse logic.
        if (type instanceof studio.mevera.imperat.backend.modern.argument.PaperNativeAware aware) {
            return aware.paperNativeType();
        }
        return getStringArgType(imperatArgument);
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
    protected com.mojang.brigadier.arguments.@org.jetbrains.annotations.Nullable ArgumentType<?> inlineFlagArgumentType() {
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
     * Routes suggestions to the Paper-native type for any Imperat-side
     * argument that opted into {@link
     * studio.mevera.imperat.backend.modern.argument.PaperNativeAware}.
     * Without this override, the base class would emit
     * Imperat-tree-derived completions for the parameter; the native type
     * already produces richer client-aware ones (selector char menu,
     * filter keys, type cycling for entity selectors; block-state keys
     * for blockState; etc.) so we delegate directly.
     */
    @Override
    protected @NotNull <BS> SuggestionProvider<BS> createSuggestionProvider(
            Command<BukkitCommandSource> command,
            Argument<BukkitCommandSource> parameter
    ) {
        if (parameter.type() instanceof studio.mevera.imperat.backend.modern.argument.PaperNativeAware aware) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            com.mojang.brigadier.arguments.ArgumentType nativeType = aware.paperNativeType();
            return (ctx, builder) -> nativeType.listSuggestions(ctx, builder);
        }
        return super.createSuggestionProvider(command, parameter);
    }
}
