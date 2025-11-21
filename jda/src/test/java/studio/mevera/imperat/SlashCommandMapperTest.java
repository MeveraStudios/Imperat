package studio.mevera.imperat;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.command.parameters.CommandParameter;

import java.util.List;

class SlashCommandMapperTest {

    private JdaImperat imperat;
    private SlashCommandMapper mapper;

    @BeforeEach
    void setup() {
        JDA jda = Mockito.mock(JDA.class);
        imperat = JdaImperat.builder(jda).build();
        mapper = new SlashCommandMapper();
    }

    @Test
    void flattensOptionalParametersAfterRequiredOnRoot() {
        Command<JdaSource> command = Command.create(imperat, "mix")
            .usage(CommandUsage.<JdaSource>builder()
                .parameters(
                    CommandParameter.requiredText("first"),
                    CommandParameter.optionalText("middle"),
                    CommandParameter.requiredInt("count"),
                    CommandParameter.optionalText("trail")
                )
                .execute((source, ctx) -> {})
            )
            .build();

        SlashCommandData data = (SlashCommandData) mapper.toSlashData(command);
        List<OptionData> options = data.getOptions();

        Assertions.assertThat(options)
            .extracting(OptionData::getName)
            .containsExactly("first", "count", "middle", "trail");
        Assertions.assertThat(options)
            .extracting(OptionData::isRequired)
            .containsExactly(true, true, false, false);
    }

    @Test
    void preservesDeepSubcommandPaths() {
        Command<JdaSource> command = Command.create(imperat, "root")
            .subCommand(
                Command.create(imperat, "alpha")
                    .usage(CommandUsage.<JdaSource>builder()
                        .parameters(CommandParameter.requiredText("alphaArg"))
                        .execute((source, ctx) -> {})
                    )
                    .subCommand(
                        Command.create(imperat, "beta")
                            .usage(CommandUsage.<JdaSource>builder()
                                .parameters(CommandParameter.requiredText("betaArg"))
                                .execute((source, ctx) -> {})
                            )
                            .subCommand(
                                Command.create(imperat, "gamma")
                                    .usage(CommandUsage.<JdaSource>builder()
                                        .parameters(CommandParameter.requiredText("gammaArg"))
                                        .execute((source, ctx) -> {})
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build();

        SlashCommandData data = (SlashCommandData) mapper.toSlashData(command);
        Assertions.assertThat(data.getSubcommandGroups()).hasSize(1);

        SubcommandGroupData group = data.getSubcommandGroups().get(0);
        Assertions.assertThat(group.getName()).isEqualTo("alpha");

        Assertions.assertThat(group.getSubcommands())
            .anySatisfy(sub -> {
                Assertions.assertThat(sub.getName()).isEqualTo("beta-gamma");
                Assertions.assertThat(sub.getOptions())
                    .extracting(OptionData::getName)
                    .contains("betaarg", "gammaarg", "alphaarg");
            });
    }

    @Test
    void combinesMultipleUsagesIntoOptionalOptions() {
        Command<JdaSource> command = Command.create(imperat, "variants")
            .usage(CommandUsage.<JdaSource>builder()
                .parameters(CommandParameter.requiredText("first"))
                .execute((source, ctx) -> {})
            )
            .usage(CommandUsage.<JdaSource>builder()
                .parameters(CommandParameter.optionalText("second"))
                .execute((source, ctx) -> {})
            )
            .build();

        SlashCommandData data = (SlashCommandData) mapper.toSlashData(command);
        List<OptionData> options = data.getOptions();

        Assertions.assertThat(options)
            .extracting(OptionData::getName)
            .containsExactly("first", "second");
        Assertions.assertThat(options)
            .extracting(OptionData::isRequired)
            .containsExactly(false, false);
    }
}
