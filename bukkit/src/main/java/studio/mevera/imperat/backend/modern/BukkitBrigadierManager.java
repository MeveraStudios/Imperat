package studio.mevera.imperat.backend.modern;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.BaseBrigadierManager;
import studio.mevera.imperat.BukkitCommandSource;
import studio.mevera.imperat.BukkitImperat;
import studio.mevera.imperat.command.arguments.Argument;

/**
 * Shared {@link BaseBrigadierManager} subclass for the non-modern-Paper
 * Brigadier paths (Paper 1.13–1.21.3 legacy event hook, and the
 * Spigot/Paper Commodore reflection bridge).
 *
 * <p>Both paths emit raw Brigadier nodes against an opaque source class
 * — Paper's {@code BukkitBrigadierCommandSource} for the legacy event,
 * NMS source for Commodore — and neither has a stable native
 * Java→Brigadier argument mapping. This class is the single home of
 * that "string-everywhere, sender-routes-through-active-backend" tree
 * builder, replacing the two private inner classes that previously
 * duplicated the implementation in
 * {@code PaperLegacyBrigadierRegistration} and {@code CommodoreRegistration}.</p>
 *
 * <p>Sender wrapping delegates to {@link BukkitImperat#wrapSender(Object)},
 * which dispatches through the active {@code RegistrationCapability} —
 * so this class is backend-agnostic by design and works under both
 * legacy paths without per-path subclasses.</p>
 *
 * @since 4.0.0
 */
public final class BukkitBrigadierManager extends BaseBrigadierManager<BukkitCommandSource> {

    private final BukkitImperat bukkitImperat;

    public BukkitBrigadierManager(@NotNull BukkitImperat bukkitImperat) {
        super(bukkitImperat);
        this.bukkitImperat = bukkitImperat;
    }

    @Override
    public BukkitCommandSource wrapCommandSource(Object commandSource) {
        return bukkitImperat.wrapSender(commandSource);
    }

    @Override
    public @NotNull com.mojang.brigadier.arguments.ArgumentType<?> getArgumentType(
            @NotNull Argument<BukkitCommandSource> imperatArgument
    ) {
        return getStringArgType(imperatArgument);
    }
}
