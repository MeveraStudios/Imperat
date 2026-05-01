package studio.mevera.imperat.backend.capability.impl;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.BukkitCommandSource;
import studio.mevera.imperat.BukkitImperat;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.adventure.AdventureProvider;
import studio.mevera.imperat.backend.capability.BukkitCapability;
import studio.mevera.imperat.backend.capability.RegistrationCapability;
import studio.mevera.imperat.backend.modern.ModernPaperBrigadierManager;
import studio.mevera.imperat.backend.modern.argument.PaperArgumentMappings;
import studio.mevera.imperat.command.Command;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link BukkitCapability#MODERN_NATIVE_BRIGADIER} registration impl.
 * Hooks Paper's {@link LifecycleEvents#COMMANDS} event and registers
 * commands via the stable {@link Commands} API.
 *
 * <h2>Timing model</h2>
 * Paper's command registrar is only valid during the {@code COMMANDS}
 * lifecycle event. {@link #initialize} hooks the event and queues every
 * {@link #registerCommand} call until the event fires; once it fires,
 * the queue drains and the registrar is captured for any subsequent
 * late registrations within the same handler scope.
 *
 * @since 4.0.0
 */
public final class ModernPaperRegistration implements RegistrationCapability {

    private final List<Command<BukkitCommandSource>> pending = new ArrayList<>();
    private BukkitImperat owner;
    private Plugin plugin;
    private AdventureProvider<CommandSender> adventureProvider;
    private ModernPaperBrigadierManager brigadierManager;
    private @Nullable Commands registrar;

    /** Reflective instantiation entrypoint — wiring happens in {@link #initialize}. */
    public ModernPaperRegistration() {
    }

    @Override
    public void initialize(@NotNull Plugin plugin,
            @NotNull BukkitImperat imperat,
            @NotNull AdventureProvider<CommandSender> adventureProvider) {
        this.owner = imperat;
        this.plugin = plugin;
        this.adventureProvider = adventureProvider;
        this.brigadierManager = new ModernPaperBrigadierManager(imperat);
        registerLifecycleHook();
    }

    private void registerLifecycleHook() {
        try {
            LifecycleEventManager<Plugin> manager = plugin.getLifecycleManager();
            manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
                this.registrar = event.registrar();
                for (Command<BukkitCommandSource> cmd : pending) {
                    brigadierManager.register(registrar, cmd);
                }
                pending.clear();
            });
        } catch (Throwable ignored) {
            // Defensive: MockBukkit / non-conforming Paper variants may not
            // implement the lifecycle manager. The pending queue stays
            // populated but Imperat's internal command tree remains usable
            // for direct execute()/autoComplete() calls — only the native
            // Brigadier registrar path is dark in that environment.
        }
    }

    @Override
    public void registerCommand(@NotNull Command<BukkitCommandSource> command) {
        if (registrar != null) {
            // Lifecycle event already fired — register immediately.
            brigadierManager.register(registrar, command);
        } else {
            pending.add(command);
        }
    }

    @Override
    public @NotNull BukkitCommandSource wrapSender(@NotNull Object sender) {
        if (sender instanceof CommandSourceStack stack) {
            // Paper-specific stack carries selector-resolver context — keep
            // the stack reference on the wrapped source so downstream
            // resolvers (entity/position) can use it.
            return new BukkitCommandSource(stack.getSender(), adventureProvider, stack);
        }
        if (sender instanceof CommandSender plain) {
            return SenderWrappers.plain(plain, adventureProvider);
        }
        throw SenderWrappers.reject(sender, "CommandSourceStack or CommandSender");
    }

    @Override
    public void applyArgumentTypeDefaults(@NotNull ImperatConfig<BukkitCommandSource> config) {
        PaperArgumentMappings.applyDefaults(config);
    }

    @Override
    public void shutdown() {
        if (adventureProvider != null) {
            adventureProvider.close();
        }
    }

    @Override
    public @NotNull AdventureProvider<CommandSender> adventureProvider() {
        return adventureProvider;
    }

    @Override
    public @NotNull BukkitCapability kind() {
        return BukkitCapability.MODERN_NATIVE_BRIGADIER;
    }
}
