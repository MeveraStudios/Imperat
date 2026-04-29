package studio.mevera.imperat.backend.modern;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.command.UnknownCommandEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.AsyncTabListener;
import studio.mevera.imperat.BukkitCommandSource;
import studio.mevera.imperat.BukkitImperat;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.Version;
import studio.mevera.imperat.adventure.AdventureProvider;
import studio.mevera.imperat.backend.BukkitBackend;
import studio.mevera.imperat.backend.modern.argument.PaperArgumentMappings;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.exception.PermissionDeniedException;

import java.util.ArrayList;
import java.util.List;

/**
 * Modern-Paper backend (1.21.4+). Registers commands through Paper's
 * {@link LifecycleEvents#COMMANDS} event using the standard
 * {@link Commands} registrar — no NMS, no Commodore.
 *
 * <h2>Timing model</h2>
 * Paper's command registrar is only valid during the {@code COMMANDS}
 * lifecycle event. The backend hooks that event eagerly at construction
 * time and queues every {@link #registerCommand(Command)} call until the
 * event fires; once it fires, the queue is drained and the registrar is
 * captured for any subsequent late registrations within the same handler
 * scope.
 *
 * <p>Late registrations (after the lifecycle handler has exited) will
 * still populate Imperat's internal command tree but cannot attach native
 * Brigadier suggestions on the Paper side. Register commands either
 * before {@code onEnable} returns, or inside an explicit lifecycle
 * handler if you need full Brigadier support.</p>
 *
 * @since 4.0.0 (Modern Paper backend)
 */
public final class ModernPaperBackend implements BukkitBackend {

    private final BukkitImperat owner;
    private final Plugin plugin;
    private final AdventureProvider<CommandSender> adventureProvider;
    private final ModernPaperBrigadierManager brigadierManager;

    /**
     * Pending commands awaiting the {@code COMMANDS} lifecycle event.
     * Drained once the event fires.
     */
    private final List<Command<BukkitCommandSource>> pending = new ArrayList<>();

    /**
     * Captured during the lifecycle event. Null until the event fires;
     * after that, holds the registrar for any late registrations within
     * the event-handler scope.
     */
    private @Nullable Commands registrar;

    public ModernPaperBackend(
            @NotNull BukkitImperat owner,
            @NotNull Plugin plugin,
            @NotNull AdventureProvider<CommandSender> adventureProvider,
            boolean rewriteUnknownCommandMessage
    ) {
        this.owner = owner;
        this.plugin = plugin;
        this.adventureProvider = adventureProvider;
        this.brigadierManager = new ModernPaperBrigadierManager(owner);

        registerLifecycleHook();
        if (rewriteUnknownCommandMessage) {
            registerUnknownCommandListener();
        }
        // Modern Paper still benefits from AsyncTabCompleteEvent for non-Brigadier
        // dispatch paths (e.g. tab completion outside the lifecycle handler scope,
        // and headless test environments where the lifecycle event never fires).
        // Brigadier client-side suggestions and this listener don't conflict.
        if (Version.SUPPORTS_PAPER_ASYNC_TAB_COMPLETION) {
            plugin.getServer().getPluginManager().registerEvents(new AsyncTabListener(owner), plugin);
        }
    }

    private void registerLifecycleHook() {
        try {
            LifecycleEventManager<Plugin> manager = plugin.getLifecycleManager();
            manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
                this.registrar = event.registrar();
                // Drain anything already queued before the event fired.
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

    private static @Nullable String stripLabel(@NotNull String commandLine) {
        String trimmed = commandLine.startsWith("/") ? commandLine.substring(1) : commandLine;
        int space = trimmed.indexOf(' ');
        String label = space >= 0 ? trimmed.substring(0, space) : trimmed;
        return label.isEmpty() ? null : label.toLowerCase();
    }

    /**
     * Wire a {@link UnknownCommandEvent} listener that routes hidden-by-
     * permissions cases through Imperat's standard error-handling pipeline.
     * When the typed label maps to a known Imperat command, the listener
     * suppresses the vanilla "Unknown command" output, builds a
     * {@link PermissionDeniedException} for the command's default pathway,
     * and dispatches it through {@code config().handleExecutionError(...)} —
     * the registered exception handler then emits the configured message.
     */
    private void registerUnknownCommandListener() {
        plugin.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
            public void onUnknown(UnknownCommandEvent event) {
                String line = event.getCommandLine();
                if (line.isEmpty()) {
                    return;
                }
                String label = stripLabel(line);
                if (label == null) {
                    return;
                }
                Command<BukkitCommandSource> imperatCommand = owner.getCommand(label);
                if (imperatCommand == null) {
                    return;
                }
                event.message(null);
                owner.execute(owner.wrapSender(event.getSender()), line);
            }
        }, plugin);
    }

    @Override
    public void registerCommand(@NotNull Command<BukkitCommandSource> command) {
        if (registrar != null) {
            // Lifecycle event already fired — register immediately.
            brigadierManager.register(registrar, command);
        } else {
            // Queue until the lifecycle hook fires.
            pending.add(command);
        }
    }

    @Override
    public @NotNull BukkitCommandSource wrapSender(@NotNull Object sender) {
        if (sender instanceof CommandSourceStack stack) {
            return new BukkitCommandSource(stack.getSender(), adventureProvider, stack);
        }
        if (sender instanceof CommandSender plain) {
            return new BukkitCommandSource(plain, adventureProvider);
        }
        throw new IllegalArgumentException(
                "Cannot wrap sender of type " + sender.getClass().getName()
                        + " — expected CommandSourceStack or CommandSender");
    }

    @Override
    public void applyArgumentTypeDefaults(@NotNull ImperatConfig<BukkitCommandSource> config) {
        PaperArgumentMappings.applyDefaults(config);
    }

    @Override
    public void shutdown() {
        adventureProvider.close();
    }

    @Override
    public @NotNull AdventureProvider<CommandSender> adventureProvider() {
        return adventureProvider;
    }
}
