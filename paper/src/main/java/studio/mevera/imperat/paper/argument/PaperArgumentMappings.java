package studio.mevera.imperat.paper.argument;

import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.paper.PaperCommandSource;

import java.util.UUID;

/**
 * Default Java-type → Paper Brigadier {@code ArgumentType} mappings,
 * applied when {@link studio.mevera.imperat.paper.PaperImperatBuilder}
 * builds an instance.
 *
 * <p>Each mapping wraps a {@link PaperArgumentType} (which carries the
 * native Paper {@code ArgumentType} + a resolver) into a
 * {@link PaperBukkitArgumentType} (Imperat-side argument type). The
 * native type is what Paper sends to the client for native suggestions;
 * the resolver translates Paper's parsed form to the friendly Java type
 * the user's command method declares.</p>
 *
 * <p>Selector / position types that depend on a runtime
 * {@code CommandContext<CommandSourceStack>} are NOT registered here —
 * they need the live context to resolve, which {@link PaperBukkitArgumentType}'s
 * parse method does not provide directly. Plugin authors who need
 * selectors should register them at use-site via
 * {@code config.registerArgType(MyType.class, customMapping)}.</p>
 *
 * @since 4.0.0 (Paper module)
 */
public final class PaperArgumentMappings {

    private PaperArgumentMappings() {
    }

    public static void applyDefaults(ImperatConfig<PaperCommandSource> config) {
        // Identity-resolved Paper types (parsed form == friendly type).
        config.registerArgType(World.class,
                new PaperBukkitArgumentType<>(World.class, PaperArgumentType.identity(ArgumentTypes.world())));

        config.registerArgType(GameMode.class,
                new PaperBukkitArgumentType<>(GameMode.class, PaperArgumentType.identity(ArgumentTypes.gameMode())));

        config.registerArgType(ItemStack.class,
                new PaperBukkitArgumentType<>(ItemStack.class, PaperArgumentType.identity(ArgumentTypes.itemStack())));

        config.registerArgType(NamespacedKey.class,
                new PaperBukkitArgumentType<>(NamespacedKey.class, PaperArgumentType.identity(ArgumentTypes.namespacedKey())));

        // Override the core's UUID with Paper's native UUID arg-type so the
        // client gets native validation + completion.
        config.registerArgType(UUID.class,
                new PaperBukkitArgumentType<>(UUID.class, PaperArgumentType.identity(ArgumentTypes.uuid())));
    }
}
