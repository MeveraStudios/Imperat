package studio.mevera.imperat.backend.modern;

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
        return getStringArgType(imperatArgument);
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
}
