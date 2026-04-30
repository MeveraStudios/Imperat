package studio.mevera.imperat.backend.capability;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.BukkitImperat;
import studio.mevera.imperat.adventure.AdventureProvider;
import studio.mevera.imperat.backend.BukkitBackend;

/**
 * Capability-aware extension of {@link BukkitBackend}. Adds reflective-
 * instantiation lifecycle (no-arg ctor + {@link #initialize}) and a
 * {@link #kind()} discriminator for diagnostics.
 *
 * <p>{@link CapabilityResolver} loads impl classes by FQN — impls
 * therefore expose a public no-arg constructor; their real wiring runs
 * inside {@code initialize}. This keeps probed-but-unselected impl
 * classes off the classloader entirely on platforms that lack their
 * dependencies (e.g. {@code ModernPaperRegistration} never loads on
 * Spigot 1.8 because the resolver never asks for it).</p>
 *
 * @since 4.0.0
 */
public interface RegistrationCapability extends BukkitBackend {

    /**
     * Bind the impl to its owning plugin + Imperat instance. Called once
     * by the resolver after the impl is instantiated reflectively.
     */
    void initialize(@NotNull Plugin plugin,
            @NotNull BukkitImperat imperat,
            @NotNull AdventureProvider<CommandSender> adventureProvider);

    /** Identifies the capability this impl satisfies — for diagnostics. */
    @NotNull BukkitCapability kind();
}
