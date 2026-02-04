package studio.mevera.imperat;

import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandUsage;

import java.util.Collections;
import java.util.List;

@ApiStatus.Internal
final class InternalBukkitCommand extends org.bukkit.command.Command implements PluginIdentifiableCommand {

    @NotNull final Command<BukkitSource> imperatCommand;
    @NotNull
    private final BukkitImperat dispatcher;

    InternalBukkitCommand(
            final @NotNull BukkitImperat dispatcher,
            final @NotNull Command<BukkitSource> imperatCommand
    ) {
        super(
                imperatCommand.name(),
                imperatCommand.description().getValueOrElse(""),
                CommandUsage.format((String) null, imperatCommand.getDefaultUsage()),
                imperatCommand.aliases()
        );
        this.dispatcher = dispatcher;
        this.imperatCommand = imperatCommand;
    }

    @Override
    public @NotNull Plugin getPlugin() {
        return dispatcher.getPlatform();
    }

    @Nullable
    @Override
    public String getPermission() {
        return imperatCommand.tree().rootNode().getPermission();
    }

    @NotNull
    @Override
    public String getDescription() {
        return super.getDescription();
    }

    @NotNull
    @Override
    public String getUsage() {
        return super.getUsage();
    }

    @Override
    public boolean execute(@NotNull CommandSender sender,
            @NotNull String label,
            String[] raw) {
        BukkitSource source = dispatcher.wrapSender(sender);
        dispatcher.execute(source, this.imperatCommand, label, raw);
        return true;
    }

    @Override
    public @NotNull List<String> tabComplete(
            final @NotNull CommandSender sender,
            final @NotNull String alias,
            final String[] args
    ) throws IllegalArgumentException {
        if (Version.SUPPORTS_PAPER_ASYNC_TAB_COMPLETION) {
            //supports async tab completion
            //we will tab complete from the async tab completion event
            return Collections.emptyList();
        }
        BukkitSource source = dispatcher.wrapSender(sender);
        StringBuilder builder = new StringBuilder(alias).append(" ");
        for (String arg : args) {
            builder.append(arg).append(" ");
        }
        if (!builder.isEmpty()) {
            builder.deleteCharAt(builder.length() - 1);
        }
        return dispatcher.autoComplete(source, builder.toString()).join();
    }

}