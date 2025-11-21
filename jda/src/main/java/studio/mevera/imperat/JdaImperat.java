package studio.mevera.imperat;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.Command;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Imperat implementation for Discord using JDA slash commands.
 */
public final class JdaImperat extends BaseImperat<JdaSource> {

    private final JDA jda;
    private final JdaSlashCommandListener listener;
    private final SlashCommandMapper slashCommandMapper = new SlashCommandMapper();

    public static JdaConfigBuilder builder(@NotNull JDA jda) {
        return new JdaConfigBuilder(jda);
    }

    JdaImperat(@NotNull JDA jda, @NotNull ImperatConfig<JdaSource> config) {
        super(config);
        this.jda = jda;
        this.listener = new JdaSlashCommandListener(this);
        this.jda.addEventListener(listener);
    }

    @Override
    public void registerCommand(Command<JdaSource> command) {
        super.registerCommand(command);
        syncCommands();
    }

    @Override
    public void unregisterCommand(String name) {
        super.unregisterCommand(name);
        syncCommands();
    }

    @Override
    public JDA getPlatform() {
        return jda;
    }

    @Override
    public void shutdownPlatform() {
        jda.removeEventListener(listener);
    }

    @Override
    public JdaSource wrapSender(Object sender) {
        return new JdaSource((SlashCommandInteractionEvent) sender);
    }

    private void syncCommands() {
        List<CommandData> data = getRegisteredCommands().stream()
            .map(slashCommandMapper::toSlashData)
            .collect(Collectors.toList());
        jda.updateCommands().addCommands(data).queue();
    }
}
