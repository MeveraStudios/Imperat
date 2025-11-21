package studio.mevera.imperat;

import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.Command;

import java.util.ArrayList;
import java.util.List;

final class JdaSlashCommandListener implements EventListener {

    private final JdaImperat imperat;

    JdaSlashCommandListener(JdaImperat imperat) {
        this.imperat = imperat;
    }

    @Override
    public void onEvent(@NotNull GenericEvent event) {
        if (event instanceof SlashCommandInteractionEvent slashEvent) {
            handleSlashCommand(slashEvent);
        }
    }

    private void handleSlashCommand(SlashCommandInteractionEvent event) {
        Command<JdaSource> command = imperat.getCommand(event.getName());
        if (command == null) {
            return;
        }

        JdaSource source = imperat.wrapSender(event);
        if (!imperat.config().getPermissionChecker().hasPermission(
            source,
            imperat.config().isAutoPermissionAssignMode()
                ? imperat.config().getPermissionLoader().load(command)
                : command.getMainPermission()
        )) {
            source.error("You don't have permission to use this command.");
            return;
        }

        List<String> arguments = new ArrayList<>();
        if (event.getSubcommandGroup() != null) {
            arguments.add(event.getSubcommandGroup());
        }
        if (event.getSubcommandName() != null) {
            arguments.add(event.getSubcommandName());
        }

        for (OptionMapping option : event.getOptions()) {
            arguments.add(mapOption(option));
        }

        imperat.execute(source, event.getName(), arguments.toArray(String[]::new));
    }

    private String mapOption(OptionMapping option) {
        return switch (option.getType()) {
            case BOOLEAN -> Boolean.toString(option.getAsBoolean());
            case INTEGER -> Long.toString(option.getAsLong());
            case NUMBER -> Double.toString(option.getAsDouble());
            case USER -> option.getAsUser().getId();
            case ROLE -> option.getAsRole().getId();
            case CHANNEL -> option.getAsChannel().getId();
            case MENTIONABLE -> option.getAsMentionable().getId();
            case ATTACHMENT -> option.getAsAttachment().getUrl();
            default -> option.getAsString();
        };
    }
}
