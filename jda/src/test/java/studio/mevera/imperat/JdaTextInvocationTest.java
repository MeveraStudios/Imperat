package studio.mevera.imperat;

import net.dv8tion.jda.api.JDA;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.arguments.Argument;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Text command parsing")
final class JdaTextInvocationTest {

    private JdaImperat<JdaCommandSource> imperat;

    @BeforeEach
    void setup() {
        imperat = JdaImperat.builder(Mockito.mock(JDA.class, Mockito.RETURNS_DEEP_STUBS)).build();
    }

    @Test
    void readsTriggerAndRemainderAfterPrefix() {
        var typed = JdaTextInvocation.read("!ban <@1> 1d", List.of("!"), null);

        assertThat(typed).isNotNull();
        assertThat(typed.trigger()).isEqualTo("ban");
        assertThat(typed.remainder()).isEqualTo("<@1> 1d");
    }

    @Test
    void triggerIsLowerCasedAndPrefixIgnoresCase() {
        var typed = JdaTextInvocation.read("AX!BAN", List.of("ax!"), null);

        assertThat(typed).isNotNull();
        assertThat(typed.trigger()).isEqualTo("ban");
        assertThat(typed.remainder()).isEmpty();
    }

    @Test
    void aSpaceAfterThePrefixIsNotACommand() {
        assertThat(JdaTextInvocation.read("! ban", List.of("!"), null)).isNull();
    }

    @Test
    void messagesWithoutThePrefixAreNotCommands() {
        assertThat(JdaTextInvocation.read("ban someone", List.of("!"), null)).isNull();
        assertThat(JdaTextInvocation.read("!", List.of("!"), null)).isNull();
        assertThat(JdaTextInvocation.read("", List.of("!"), null)).isNull();
    }

    @Test
    void theLongestMatchingPrefixWins() {
        var typed = JdaTextInvocation.read("!!b x", List.of("!", "!!"), null);

        assertThat(typed).isNotNull();
        assertThat(typed.trigger()).isEqualTo("b");
    }

    @Test
    void anEmptyPrefixReadsTheFirstWord() {
        var typed = JdaTextInvocation.read("hello there  world", List.of(""), null);

        assertThat(typed).isNotNull();
        assertThat(typed.trigger()).isEqualTo("hello");
        assertThat(typed.remainder()).isEqualTo("there  world");
    }

    @Test
    void aMentionOfTheBotWorksAsAPrefix() {
        assertThat(JdaTextInvocation.read("<@42> ban x", List.of("!"), "42").trigger()).isEqualTo("ban");
        assertThat(JdaTextInvocation.read("<@!42>ban x", List.of(), "42").trigger()).isEqualTo("ban");
        assertThat(JdaTextInvocation.read("<@43> ban x", List.of(), "42")).isNull();
    }

    @Test
    void pathNamesSplitSlashStyleGroups() {
        assertThat(JdaTextInvocation.pathNames(" ticket staff add-all ")).containsExactly("ticket", "staff", "add", "all");
        assertThat(JdaTextInvocation.pathNames("ban")).containsExactly("ban");
        assertThat(JdaTextInvocation.pathNames("  ")).isEmpty();
    }

    @Test
    void resolveFollowsThePathAndTheTypedSubcommands() {
        Command<JdaCommandSource> ticket = Command.create(imperat, "ticket")
                                                   .subCommand(Command.create(imperat, "staff")
                                                                       .subCommand(Command.create(imperat, "add")
                                                                                           .pathway(CommandPathway.<JdaCommandSource>builder()
                                                                                                            .arguments(Argument.requiredText("role"))
                                                                                                            .execute((source, ctx) -> {
                                                                                                            }))
                                                                                           .build())
                                                                       .build())
                                                   .build();

        var resolved = JdaTextInvocation.resolve(ticket, List.of("ticket", "staff"), "add <@&5>");

        assertThat(resolved.input()).isEqualTo("staff add <@&5>");
        assertThat(resolved.commandPath()).isEqualTo("ticket staff add");
    }

    @Test
    void resolveStopsAtTheFirstWordThatIsNotASubcommand() {
        Command<JdaCommandSource> ban = Command.create(imperat, "ban")
                                                .pathway(CommandPathway.<JdaCommandSource>builder()
                                                                 .arguments(Argument.requiredText("user"))
                                                                 .execute((source, ctx) -> {
                                                                 }))
                                                .build();

        var resolved = JdaTextInvocation.resolve(ban, List.of("ban"), "<@1>");

        assertThat(resolved.input()).isEqualTo("<@1>");
        assertThat(resolved.commandPath()).isEqualTo("ban");
    }

    @Test
    void deepPathsAreWrittenLikeSlashFullNames() {
        Command<JdaCommandSource> root = Command.create(imperat, "a")
                                                 .subCommand(Command.create(imperat, "b")
                                                                     .subCommand(Command.create(imperat, "c")
                                                                                         .subCommand(Command.create(imperat, "d")
                                                                                                             .pathway(CommandPathway.<JdaCommandSource>builder()
                                                                                                                              .execute((source, ctx) -> {
                                                                                                                              }))
                                                                                                             .build())
                                                                                         .build())
                                                                     .build())
                                                 .build();

        assertThat(JdaTextInvocation.resolve(root, List.of("a"), "b c d").commandPath()).isEqualTo("a b c-d");
    }
}
