package studio.mevera.imperat.paper;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.command.UnknownCommandEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.BaseImperat;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.adventure.AdventureProvider;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.exception.PermissionDeniedException;
import studio.mevera.imperat.paper.brigadier.PaperBrigadierManager;
import studio.mevera.imperat.util.ImperatDebugger;

import java.util.ArrayList;
import java.util.List;

/**
 * Modern-Paper Imperat (1.21.4+). Registers commands through Paper's
 * {@link LifecycleEvents#COMMANDS} event using the standard
 * {@link Commands} registrar — no NMS, no Commodore.
 *
 * <h2>Timing model</h2>
 * Paper's command registrar is only valid during the {@code COMMANDS}
 * lifecycle event. {@link #builder(Plugin)} hooks that event eagerly at
 * build time and queues every {@link #registerSimpleCommand} call until
 * the event fires; once it fires, the queue is drained and the registrar
 * is captured for any subsequent late registrations within the same
 * handler scope.
 *
 * <p>Late registrations (after the lifecycle handler has exited) will
 * still populate Imperat's internal command tree but cannot attach native
 * Brigadier suggestions on the Paper side. Register commands either
 * before {@code onEnable} returns, or inside an explicit lifecycle
 * handler if you need full Brigadier support.</p>
 *
 * <p>For Spigot / pre-1.21 servers, use the legacy BukkitImperat which uses Commodore +
 * NMS reflection.</p>
 *
 * @since 4.0.0 (Paper module)
 */
public final class PaperImperat extends BaseImperat<PaperCommandSource> {

    private final Plugin plugin;
    private final AdventureProvider<CommandSender> adventureProvider;
    private final PaperBrigadierManager brigadierManager;

    /**
     * Pending commands awaiting the {@code COMMANDS} lifecycle event.
     * Drained once the event fires.
     */
    private final List<Command<PaperCommandSource>> pending = new ArrayList<>();

    /**
     * Captured during the lifecycle event. Null until the event fires;
     * after that, holds the registrar for any late registrations within
     * the event-handler scope.
     */
    private @Nullable Commands registrar;

    PaperImperat(
            @NotNull Plugin plugin,
            @NotNull AdventureProvider<CommandSender> adventureProvider,
            @NotNull ImperatConfig<PaperCommandSource> config,
            boolean rewriteUnknownCommandMessage
    ) {
        super(config);
        this.plugin = plugin;
        this.adventureProvider = adventureProvider;
        this.brigadierManager = new PaperBrigadierManager(this);

        ImperatDebugger.setLogger(plugin.getLogger());
        registerLifecycleHook();
        if (rewriteUnknownCommandMessage) {
            registerUnknownCommandListener();
        }
    }

    public static PaperImperatBuilder builder(@NotNull Plugin plugin) {
        return new PaperImperatBuilder(plugin);
    }

    private void registerLifecycleHook() {
        LifecycleEventManager<Plugin> manager = plugin.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            this.registrar = event.registrar();
            // Drain anything already queued before the event fired.
            for (Command<PaperCommandSource> cmd : pending) {
                brigadierManager.register(registrar, cmd);
            }
            pending.clear();
        });
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
     * the registered {@link studio.mevera.imperat.exception.CommandExceptionHandler}
     * for {@code PermissionDeniedException} (default or user-supplied) then
     * emits the configured message.
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
                Command<PaperCommandSource> imperatCommand = getCommand(label);
                if (imperatCommand == null) {
                    return;
                }
                event.message(null);
                PaperImperat.this.execute(wrapSender(event.getSender()), line);
            }
        }, plugin);
    }

    @Override
    public PaperCommandSource createDummySender() {
        return new PaperCommandSource(Bukkit.getConsoleSender(), adventureProvider);
    }

    @Override
    public PaperCommandSource wrapSender(Object sender) {
        if (sender instanceof CommandSourceStack stack) {
            return new PaperCommandSource(stack, adventureProvider);
        }
        if (sender instanceof CommandSender plain) {
            return new PaperCommandSource(plain, adventureProvider);
        }
        throw new IllegalArgumentException(
                "Cannot wrap sender of type " + sender.getClass().getName() + " — expected CommandSourceStack or CommandSender");
    }

    @Override
    public Plugin getPlatform() {
        return plugin;
    }

    @Override
    public void shutdownPlatform() {
        this.adventureProvider.close();
    }

    @Override
    public void registerSimpleCommand(Command<PaperCommandSource> command) {
        super.registerSimpleCommand(command);
        if (registrar != null) {
            // Lifecycle event already fired — register immediately.
            brigadierManager.register(registrar, command);
        } else {
            // Queue until the lifecycle hook fires.
            pending.add(command);
        }
    }

    public AdventureProvider<CommandSender> getAdventureProvider() {
        return adventureProvider;
    }
}
