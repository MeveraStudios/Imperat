package studio.mevera.imperat;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.Command;

import java.util.ArrayList;
import java.util.List;

public class HytaleImperat extends BaseImperat<HytaleCommandSource> {

    protected final JavaPlugin plugin;

    HytaleImperat(JavaPlugin plugin, @NotNull ImperatConfig<HytaleCommandSource> config) {
        super(config);
        this.plugin = plugin;
    }

    public static HytaleConfigBuilder builder(JavaPlugin plugin) {
        return new HytaleConfigBuilder(plugin);
    }

    @Override
    public JavaPlugin getPlatform() {
        return plugin;
    }

    @Override
    public void shutdownPlatform() {
        HytaleServer.get().getPluginManager()
                .unload(plugin.getIdentifier());
    }

    @Override
    public void registerSimpleCommand(Command<HytaleCommandSource> command) {
        super.registerSimpleCommand(command);
        registerHytaleCommand(
                new InternalHytaleCommand(this, command)
        );
    }

    void registerHytaleCommand(InternalHytaleCommand hytaleCommand) {
        plugin.getCommandRegistry().registerCommand(hytaleCommand);
    }

    @Override
    public void unregisterCommand(String name) {
        final Command<HytaleCommandSource> command = getCommand(name);
        if (command == null) {
            return;
        }

        List<String> aliases = new ArrayList<>(command.aliases());
        aliases.addFirst(name.toLowerCase());

        var commandManager = HytaleServer.get().getCommandManager();
        aliases.forEach(
                (alias) -> commandManager.getCommandRegistration().remove(alias.toLowerCase())
        );

        super.unregisterCommand(name);
    }

    @Override
    public HytaleCommandSource createDummySender() {
        return new HytaleCommandSource(ConsoleSender.INSTANCE);
    }

    @Override
    public HytaleCommandSource wrapSender(Object sender) {
        if (!(sender instanceof CommandSender cmdSender)) {
            throw new IllegalArgumentException("Sender object is not of type '" + CommandSender.class.getName() + "'");
        }
        return new HytaleCommandSource(cmdSender);
    }
}


