package studio.mevera.imperat;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.SimpleArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.exception.CommandException;

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
        Command<JdaCommandSource> command = Command.create(imperat, "mix")
                                                    .pathway(CommandPathway.<JdaCommandSource>builder()
                                                                     .arguments(
                                                                    Argument.requiredText("first"),
                                                                    Argument.requiredInt("count"),
                                                                    Argument.optionalText("middle"),
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
                .containsExactly("first", "count", "middle", "trail");
    }

    @Test
    void preservesDeepSubcommandPaths() {
        Command<JdaCommandSource> command = Command.create(imperat, "root")
                                             .subCommand(
                                                     Command.create(imperat, "alpha")
                                                             .pathway(CommandPathway.<JdaCommandSource>builder()
                                                                              .arguments(Argument.requiredText("alphaArg"))
                                                                            .execute((source, ctx) -> {
                                                                            })
                                                             )
                                                             .subCommand(
                                                                     Command.create(imperat, "beta")
                                                                             .pathway(CommandPathway.<JdaCommandSource>builder()
                                                                                              .arguments(Argument.requiredText("betaArg"))
                                                                                            .execute((source, ctx) -> {
                                                                                            })
                                                                             )
                                                                             .subCommand(
                                                                                     Command.create(imperat, "gamma")
                                                                                             .pathway(CommandPathway.<JdaCommandSource>builder()
                                                                                                              .arguments(Argument.requiredText(
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
        Command<JdaCommandSource> command = Command.create(imperat, "variants")
                                                    .pathway(CommandPathway.<JdaCommandSource>builder()
                                                                     .arguments(Argument.requiredText("first"))
                                                            .execute((source, ctx) -> {
                                                            })
                                             )
                                                    .pathway(CommandPathway.<JdaCommandSource>builder()
                                                                     .arguments(Argument.optionalText("second"))
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

    @Test
    void restrictsDiscordChannelOptionsToTheRequestedChannelType() {
        assertChannelType(Category.class, ChannelType.CATEGORY);
        assertChannelType(TextChannel.class, ChannelType.TEXT);
        assertChannelType(VoiceChannel.class, ChannelType.VOICE);
    }

    private <T> void assertChannelType(Class<T> channelClass, ChannelType channelType) {
        Command<JdaCommandSource> command = Command.create(imperat, "channel")
                                                    .pathway(CommandPathway.<JdaCommandSource>builder()
                                                                     .arguments(Argument.required(
                                                                             "channel",
                                                                             new SimpleArgumentType<JdaCommandSource, T>(channelClass) {
                                                                                 @Override
                                                                                 public T parse(
                                                                                         @NotNull CommandContext<JdaCommandSource> context,
                                                                                         @NotNull Argument<JdaCommandSource> argument,
                                                                                         @NotNull String input
                                                                                 ) throws CommandException {
                                                                                     return null;
                                                                                 }
                                                                             }
                                                                     ))
                                                                     .execute((source, ctx) -> {
                                                                     }))
                                                    .build();

        OptionData option = mapper.mapCommand(command).commandData().getOptions().get(0);
        Assertions.assertThat(option.getType()).isEqualTo(OptionType.CHANNEL);
        Assertions.assertThat(option.getChannelTypes()).containsExactly(channelType);
    }
}
