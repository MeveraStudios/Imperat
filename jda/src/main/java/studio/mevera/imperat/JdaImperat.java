package studio.mevera.imperat;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.Command;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Imperat implementation for Discord using JDA slash commands.
 */
public final class JdaImperat extends BaseImperat<JdaSource> {

    private final JDA jda;
    private final JdaSlashCommandListener listener;
    private final SlashCommandMapper slashCommandMapper = new SlashCommandMapper();
    private final Map<String, SlashCommandMapper.SlashMapping> slashMappings = new ConcurrentHashMap<>();

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

    SlashCommandMapper.SlashMapping getSlashMapping(String name) {
        return slashMappings.get(name.toLowerCase());
    }

    private void syncCommands() {
        List<SlashCommandMapper.SlashMapping> mappings = getRegisteredCommands().stream()
                .map(slashCommandMapper::mapCommand)
                .toList();

        slashMappings.clear();
        mappings.forEach(mapping -> slashMappings.put(mapping.commandName(), mapping));

        List<CommandData> data = mappings.stream()
                .map(SlashCommandMapper.SlashMapping::commandData)
                .collect(Collectors.toList());
        jda.updateCommands().addCommands(data).queue();
    }
}