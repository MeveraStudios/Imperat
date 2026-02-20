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
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.parameters.Argument;

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
                                             .usage(CommandPathway.<JdaSource>builder()
                                                            .parameters(
                                                                    Argument.requiredText("first"),
                                                                    Argument.optionalText("middle"),
                                                                    Argument.requiredInt("count"),
                                                                    Argument.optionalText("trail")
                                                            )
                                                            .execute((source, ctx) -> {
                                                            })
                                             )
                                             .build();

        SlashCommandMapper.SlashMapping mapping = mapper.mapCommand(command);
        SlashCommandData data = mapping.commandData();
        List<OptionData> options = data.getOptions();

        Assertions.assertThat(options)
                .extracting(OptionData::getName)
                .containsExactly("first", "count", "middle", "trail");
        Assertions.assertThat(options)
                .extracting(OptionData::isRequired)
                .containsExactly(true, true, false, false);

        SlashCommandMapper.Invocation invocation = mapping.invocationFor(null, null);
        Assertions.assertThat(invocation.optionOrder())
                .containsExactly("first", "middle", "count", "trail");
    }

    @Test
    void preservesDeepSubcommandPaths() {
        Command<JdaSource> command = Command.create(imperat, "root")
                                             .subCommand(
                                                     Command.create(imperat, "alpha")
                                                             .usage(CommandPathway.<JdaSource>builder()
                                                                            .parameters(Argument.requiredText("alphaArg"))
                                                                            .execute((source, ctx) -> {
                                                                            })
                                                             )
                                                             .subCommand(
                                                                     Command.create(imperat, "beta")
                                                                             .usage(CommandPathway.<JdaSource>builder()
                                                                                            .parameters(Argument.requiredText("betaArg"))
                                                                                            .execute((source, ctx) -> {
                                                                                            })
                                                                             )
                                                                             .subCommand(
                                                                                     Command.create(imperat, "gamma")
                                                                                             .usage(CommandPathway.<JdaSource>builder()
                                                                                                            .parameters(Argument.requiredText(
                                                                                                                    "gammaArg"))
                                                                                                            .execute((source, ctx) -> {
                                                                                                            })
                                                                                             )
                                                                                             .build()
                                                                             )
                                                                             .build()
                                                             )
                                                             .build()
                                             )
                                             .build();

        SlashCommandMapper.SlashMapping mapping = mapper.mapCommand(command);
        SlashCommandData data = mapping.commandData();
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

        SlashCommandMapper.Invocation invocation = mapping.invocationFor("alpha", "beta-gamma");
        Assertions.assertThat(invocation.path()).containsExactly("alpha", "beta", "gamma");
        Assertions.assertThat(invocation.optionOrder())
                .containsExactly("alphaarg", "betaarg", "gammaarg");
    }

    @Test
    void combinesMultipleGetAllPossiblePathwaysIntoOptionalOptions() {
        Command<JdaSource> command = Command.create(imperat, "variants")
                                             .usage(CommandPathway.<JdaSource>builder()
                                                            .parameters(Argument.requiredText("first"))
                                                            .execute((source, ctx) -> {
                                                            })
                                             )
                                             .usage(CommandPathway.<JdaSource>builder()
                                                            .parameters(Argument.optionalText("second"))
                                                            .execute((source, ctx) -> {
                                                            })
                                             )
                                             .build();

        SlashCommandMapper.SlashMapping mapping = mapper.mapCommand(command);
        SlashCommandData data = mapping.commandData();
        List<OptionData> options = data.getOptions();

        Assertions.assertThat(options)
                .extracting(OptionData::getName)
                .containsExactly("first", "second");
        Assertions.assertThat(options)
                .extracting(OptionData::isRequired)
                .containsExactly(false, false);
    }
}
