package studio.mevera.imperat;

import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.help.GenericCommandHelpTopic;
import org.bukkit.help.HelpTopic;
import org.bukkit.help.HelpTopicFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.context.ArgumentInput;
import studio.mevera.imperat.help.CommandHelp;

import java.util.ArrayList;
import java.util.List;

final class ImperatBukkitHelpTopic extends GenericCommandHelpTopic {

    private final BukkitImperat imperat;

    public ImperatBukkitHelpTopic(InternalBukkitCommand command, BukkitImperat bukkitImperat) {
        super(command);
        this.imperat = bukkitImperat;

        var factory = imperat.config.getContextFactory();

        List<String> messages = new ArrayList<>();

        HelpSource source = new HelpSource(imperat.wrapSender(Bukkit.getConsoleSender()), messages);
        var context = factory.createContext(imperat, source, command.imperatCommand, command.imperatCommand.name(), ArgumentInput.empty());
        var resolvedContext = factory.createExecutionContext(context, command.imperatCommand.getDefaultUsage());
        CommandHelp commandHelp = new CommandHelp(imperat.config, resolvedContext);
        commandHelp.display(source);

        this.fullText = String.join("\n", messages);
    }

    @Override
    public @NotNull String getFullText(@NotNull CommandSender sender) {
        return super.getFullText(sender);
    }

    @Override
    public boolean canSee(@NotNull CommandSender sender) {
        // Define whether the sender has permission to see this help topic
        return imperat.config.getPermissionResolver()
                .hasPermission(imperat.wrapSender(sender), command.getPermission()); // Example: always visible
    }

    static class HelpSource extends BukkitSource {

        private final List<String> messages;
        private final LegacyComponentSerializer componentSerializer = LegacyComponentSerializer.legacySection();
        protected HelpSource(BukkitSource source, List<String> messages) {
            super(source.sender, source.provider);
            this.messages = messages;
        }

        /**
         * Replies to the command sender with a string message
         *
         * @param message the message
         */
        @Override
        public void reply(String message) {
            messages.add(message);
        }

        /**
         * Replies to the command sender with a component message
         *
         * @param component the message component
         */
        @Override
        public void reply(ComponentLike component) {
            messages.add(componentSerializer.serialize(component.asComponent()));
        }
    }

    static class Factory implements HelpTopicFactory<InternalBukkitCommand> {

        private final BukkitImperat imperat;

        Factory(BukkitImperat imperat) {
            this.imperat = imperat;
        }

        @Override
        public @Nullable HelpTopic createTopic(@NotNull InternalBukkitCommand internalBukkitCommand) {
            return new ImperatBukkitHelpTopic(internalBukkitCommand,imperat);
        }
    }
}