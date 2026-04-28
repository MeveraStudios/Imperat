package studio.mevera.imperat;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.FlagArgumentBuilder;
import studio.mevera.imperat.command.arguments.type.ArgumentTypes;

import java.util.List;

/**
 * Phase 6b regression coverage: every {@code @Switch} and {@code @Flag} on
 * an Imperat pathway must surface as a slash-command option, otherwise
 * Discord users have no way to invoke them.
 *
 * <p>Pre-fix: {@code SlashCommandMapper} silently dropped flags entirely —
 * a user writing {@code @Switch("verbose") boolean verbose} got a slash
 * command with no toggle, with no error at registration time.</p>
 */
@DisplayName("Slash Command Flag Support")
final class SlashCommandFlagSupportTest {

    private JdaImperat imperat;
    private SlashCommandMapper mapper;

    @BeforeEach
    void setup() {
        JDA jda = Mockito.mock(JDA.class);
        imperat = JdaImperat.builder(jda).build();
        mapper = new SlashCommandMapper();
    }

    @Test
    @DisplayName("Switch flag surfaces as a BOOLEAN slash option")
    void switchFlagBecomesBooleanOption() {
        Command<JdaCommandSource> command = Command.create(imperat, "broadcast")
                                                    .pathway(CommandPathway.<JdaCommandSource>builder()
                                                                     .arguments(Argument.requiredText("message"))
                                                                     .withFlags(FlagArgumentBuilder.<JdaCommandSource, Object>ofSwitch("silent")
                                                                                        .aliases("s"))
                                                                     .execute((source, ctx) -> {
                                                                     }))
                                                    .build();

        SlashCommandMapper.SlashMapping mapping = mapper.mapCommand(command);
        SlashCommandData data = mapping.commandData();
        List<OptionData> options = data.getOptions();

        Assertions.assertThat(options)
                .as("Switch flag must appear as a slash option")
                .extracting(OptionData::getName)
                .contains("silent");

        OptionData silent = options.stream()
                                    .filter(o -> o.getName().equals("silent"))
                                    .findFirst()
                                    .orElseThrow();
        Assertions.assertThat(silent.getType()).isEqualTo(OptionType.BOOLEAN);
        Assertions.assertThat(silent.isRequired())
                .as("Flags are always optional in slash terms")
                .isFalse();
    }

    @Test
    @DisplayName("Value flag with String input surfaces as a STRING slash option")
    void stringValueFlagBecomesStringOption() {
        Command<JdaCommandSource> command = Command.create(imperat, "ban")
                                                    .pathway(CommandPathway.<JdaCommandSource>builder()
                                                                     .arguments(Argument.requiredText("target"))
                                                                     .withFlags(FlagArgumentBuilder.ofFlag("reason",
                                                                             ArgumentTypes.<JdaCommandSource>string()))
                                                                     .execute((source, ctx) -> {
                                                                     }))
                                                    .build();

        SlashCommandMapper.SlashMapping mapping = mapper.mapCommand(command);
        SlashCommandData data = mapping.commandData();

        OptionData reason = data.getOptions().stream()
                                    .filter(o -> o.getName().equals("reason"))
                                    .findFirst()
                                    .orElseThrow(() -> new AssertionError(
                                            "Value flag must surface as a slash option"));
        Assertions.assertThat(reason.getType()).isEqualTo(OptionType.STRING);
        Assertions.assertThat(reason.isRequired()).isFalse();
    }

    @Test
    @DisplayName("Value flag with Integer input surfaces as an INTEGER slash option")
    void integerValueFlagBecomesIntegerOption() {
        Command<JdaCommandSource> command = Command.create(imperat, "mute")
                                                    .pathway(CommandPathway.<JdaCommandSource>builder()
                                                                     .arguments(Argument.requiredText("target"))
                                                                     .withFlags(FlagArgumentBuilder.ofFlag("minutes",
                                                                             ArgumentTypes.<JdaCommandSource, Integer>numeric(Integer.class)))
                                                                     .execute((source, ctx) -> {
                                                                     }))
                                                    .build();

        SlashCommandMapper.SlashMapping mapping = mapper.mapCommand(command);
        SlashCommandData data = mapping.commandData();

        OptionData minutes = data.getOptions().stream()
                                     .filter(o -> o.getName().equals("minutes"))
                                     .findFirst()
                                     .orElseThrow();
        Assertions.assertThat(minutes.getType()).isEqualTo(OptionType.INTEGER);
        Assertions.assertThat(minutes.isRequired()).isFalse();
    }

    @Test
    @DisplayName("Flags appear after positional arguments in the option list")
    void flagsAppearAfterPositionalArgs() {
        Command<JdaCommandSource> command = Command.create(imperat, "kick")
                                                    .pathway(CommandPathway.<JdaCommandSource>builder()
                                                                     .arguments(Argument.requiredText("target"))
                                                                     .withFlags(FlagArgumentBuilder.<JdaCommandSource, Object>ofSwitch("force"))
                                                                     .execute((source, ctx) -> {
                                                                     }))
                                                    .build();

        SlashCommandMapper.SlashMapping mapping = mapper.mapCommand(command);
        SlashCommandData data = mapping.commandData();

        Assertions.assertThat(data.getOptions())
                .extracting(OptionData::getName)
                .as("Required positional first, then flags as optional")
                .containsExactly("target", "force");
        Assertions.assertThat(data.getOptions())
                .extracting(OptionData::isRequired)
                .containsExactly(true, false);
    }

    @Test
    @DisplayName("Multiple flags on the same pathway all surface")
    void multipleFlagsAllSurface() {
        Command<JdaCommandSource> command = Command.create(imperat, "post")
                                                    .pathway(CommandPathway.<JdaCommandSource>builder()
                                                                     .arguments(Argument.requiredText("title"))
                                                                     .withFlags(
                                                                             FlagArgumentBuilder.<JdaCommandSource, Object>ofSwitch("pinned"),
                                                                             FlagArgumentBuilder.<JdaCommandSource, Object>ofSwitch("anonymous"),
                                                                             FlagArgumentBuilder.ofFlag("category",
                                                                                     ArgumentTypes.<JdaCommandSource>string())
                                                                     )
                                                                     .execute((source, ctx) -> {
                                                                     }))
                                                    .build();

        SlashCommandMapper.SlashMapping mapping = mapper.mapCommand(command);
        SlashCommandData data = mapping.commandData();

        Assertions.assertThat(data.getOptions())
                .extracting(OptionData::getName)
                .contains("pinned", "anonymous", "category");
    }
}
