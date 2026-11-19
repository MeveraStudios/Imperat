package studio.mevera.imperat;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.arguments.Argument;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Text commands")
final class JdaTextCommandsTest {

    private JDA jda;
    private final AtomicReference<JdaCommandSource> ranAs = new AtomicReference<>();
    private final List<String> ranWith = new ArrayList<>();

    @BeforeEach
    void setup() {
        jda = mock(JDA.class, RETURNS_DEEP_STUBS);
        when(jda.getSelfUser().getId()).thenReturn("42");
    }

    private JdaImperat<JdaCommandSource> imperat(JdaTextCommands.Builder text) {
        JdaImperat<JdaCommandSource> imperat = JdaImperat.builder(jda).textCommands(text.typing(false).build()).build();
        imperat.registerSimpleCommand(Command.create(imperat, "ban")
                                              .pathway(CommandPathway.<JdaCommandSource>builder()
                                                               .arguments(Argument.requiredText("user"), Argument.optionalGreedy("reason"))
                                                               .execute((source, ctx) -> {
                                                                   ranAs.set(source);
                                                                   ranWith.add(ctx.getArgument("user"));
                                                                   ranWith.add(ctx.getArgument("reason"));
                                                               }))
                                              .build());
        imperat.registerSimpleCommand(Command.create(imperat, "ticket")
                                              .subCommand(Command.create(imperat, "staff")
                                                                  .subCommand(Command.create(imperat, "add")
                                                                                      .pathway(CommandPathway.<JdaCommandSource>builder()
                                                                                                       .arguments(Argument.requiredText("role"))
                                                                                                       .execute((source, ctx) -> {
                                                                                                           ranAs.set(source);
                                                                                                           ranWith.add(ctx.getArgument("role"));
                                                                                                       }))
                                                                                      .build())
                                                                  .build())
                                              .build());
        return imperat;
    }

    private MessageReceivedEvent message(String content, boolean fromBot) {
        MessageReceivedEvent event = mock(MessageReceivedEvent.class, RETURNS_DEEP_STUBS);
        when(event.getJDA()).thenReturn(jda);
        when(event.getMessage().getContentRaw()).thenReturn(content);
        when(event.getAuthor().isBot()).thenReturn(fromBot);
        when(event.isWebhookMessage()).thenReturn(false);
        when(event.isFromGuild()).thenReturn(true);
        return event;
    }

    @Test
    void aCustomTriggerRunsItsCommandWithTheRestAsArguments() {
        var imperat = imperat(JdaTextCommands.builder().prefixes("!")
                                      .triggers((event, trigger) -> trigger.equals("b") ? "ban" : null));

        assertThat(imperat.dispatchText(message("!b <@1> spamming a lot", false))).isTrue();

        assertThat(ranWith).containsExactly("<@1>", "spamming a lot");
        assertThat(ranAs.get().isMessage()).isTrue();
        assertThat(ranAs.get().label()).isEqualTo("b");
        assertThat(ranAs.get().commandPath()).isEqualTo("ban");
    }

    @Test
    void commandNamesWorkUnlessTurnedOff() {
        assertThat(imperat(JdaTextCommands.builder()).dispatchText(message("!ban x", false))).isTrue();
        assertThat(imperat(JdaTextCommands.builder().commandNames(false)).dispatchText(message("!ban x", false))).isFalse();
    }

    @Test
    void aTriggerForASubcommandTakesTheTypedSubcommandsAfterIt() {
        var imperat = imperat(JdaTextCommands.builder()
                                      .triggers((event, trigger) -> trigger.equals("ts") ? "ticket staff" : null));

        assertThat(imperat.dispatchText(message("!ts add <@&5>", false))).isTrue();

        assertThat(ranWith).containsExactly("<@&5>");
        assertThat(ranAs.get().commandPath()).isEqualTo("ticket staff add");
    }

    @Test
    void botsUnknownWordsAndFilteredMessagesRunNothing() {
        assertThat(imperat(JdaTextCommands.builder()).dispatchText(message("!ban x", true))).isFalse();
        assertThat(imperat(JdaTextCommands.builder()).dispatchText(message("!kick x", false))).isFalse();
        assertThat(imperat(JdaTextCommands.builder().filter(event -> false)).dispatchText(message("!ban x", false))).isFalse();
        assertThat(ranAs.get()).isNull();
    }

    @Test
    void withoutTextCommandsMessagesAreIgnored() {
        JdaImperat<JdaCommandSource> imperat = JdaImperat.builder(jda).build();

        assertThat(imperat.getTextCommands()).isNull();
        assertThat(imperat.dispatchText(message("!ban x", false))).isFalse();
    }

    @Test
    void aTriggerToARemovedCommandRunsNothing() {
        var imperat = imperat(JdaTextCommands.builder().triggers((event, trigger) -> "gone"));

        assertThat(imperat.dispatchText(message("!anything", false))).isFalse();
    }

    // ---------------------------------------------------------------- answering

    @Test
    void aTextCommandAnswersByReplyingToTheMessage() {
        MessageReceivedEvent event = message("!ban x", false);
        Message reply = mock(Message.class);
        when(event.getMessage().reply(any(MessageCreateData.class)).mentionRepliedUser(false).failOnInvalidReply(false).submit())
                .thenReturn(CompletableFuture.completedFuture(reply));
        JdaCommandSource source = JdaCommandSource.ofMessage(event, "ban", "ban", null);

        assertThat(source.send(MessageCreateData.fromContent("done")).join()).isSameAs(reply);
    }

    @Test
    void aTextCommandAnswersPrivatelyThroughItsReplier() {
        MessageReceivedEvent event = message("!ban x", false);
        List<MessageCreateData> privately = new ArrayList<>();
        JdaCommandSource source = JdaCommandSource.ofMessage(event, "ban", "ban", (e, data) -> {
            privately.add(data);
            return CompletableFuture.completedFuture(null);
        });

        source.sendPrivately(MessageCreateData.fromContent("secret")).join();

        assertThat(privately).extracting(MessageCreateData::getContent).containsExactly("secret");
    }

    @Test
    void aSlashCommandAnswersPrivatelyByReplacingThePublicThinking() {
        SlashCommandInteractionEvent event = mock(SlashCommandInteractionEvent.class, RETURNS_DEEP_STUBS);
        InteractionHook hook = mock(InteractionHook.class, RETURNS_DEEP_STUBS);
        WebhookMessageCreateAction<Message> action = mock(WebhookMessageCreateAction.class, RETURNS_DEEP_STUBS);
        when(event.isAcknowledged()).thenReturn(true);
        when(event.getHook()).thenReturn(hook);
        when(hook.deleteOriginal().submit()).thenReturn(CompletableFuture.completedFuture(null));
        when(hook.sendMessage(any(MessageCreateData.class))).thenReturn(action);
        when(action.setEphemeral(true)).thenReturn(action);
        when(action.submit()).thenReturn(CompletableFuture.completedFuture(mock(Message.class)));
        Mockito.clearInvocations(hook, action);
        JdaCommandSource source = JdaCommandSource.ofSlash(event);

        source.sendPrivately(MessageCreateData.fromContent("one")).join();
        source.sendPrivately(MessageCreateData.fromContent("two")).join();

        verify(hook, Mockito.times(1)).deleteOriginal();
        verify(action, Mockito.times(2)).setEphemeral(true);
    }

    @Test
    void aSlashCommandsFirstPublicAnswerKeepsTheThinkingMessage() {
        SlashCommandInteractionEvent event = mock(SlashCommandInteractionEvent.class, RETURNS_DEEP_STUBS);
        InteractionHook hook = mock(InteractionHook.class, RETURNS_DEEP_STUBS);
        when(event.isAcknowledged()).thenReturn(true);
        when(event.getHook()).thenReturn(hook);
        JdaCommandSource source = JdaCommandSource.ofSlash(event);

        source.send(MessageCreateData.fromContent("public"));
        source.sendPrivately(MessageCreateData.fromContent("later"));

        verify(hook, never()).deleteOriginal();
    }

    @Test
    void mentionedUsersAreFoundWithoutTheCache() {
        MessageReceivedEvent event = message("!ban <@100000000000000001>", false);
        User mentioned = mock(User.class);
        when(mentioned.getIdLong()).thenReturn(100000000000000001L);
        when(event.getMessage().getMentions().getUsers()).thenReturn(List.of(mentioned));
        JdaCommandSource source = JdaCommandSource.ofMessage(event, "ban", "ban", null);

        assertThat(source.findUser(100000000000000001L)).isSameAs(mentioned);
    }

    // ---------------------------------------------------------------- the reveal button

    @Test
    void theRevealButtonShowsTheAnswerOnlyToTheMemberWhoRanTheCommand() {
        MessageReceivedEvent event = message("!balance", false);
        when(event.getAuthor().getIdLong()).thenReturn(7L);
        when(event.getAuthor().getAsMention()).thenReturn("<@7>");
        ArgumentCaptor<MessageCreateData> prompt = ArgumentCaptor.forClass(MessageCreateData.class);
        Message sent = mock(Message.class, RETURNS_DEEP_STUBS);
        when(event.getMessage().reply(prompt.capture()).mentionRepliedUser(false).failOnInvalidReply(false).submit())
                .thenReturn(CompletableFuture.completedFuture(sent));
        MessageCreateData answer = MessageCreateData.fromContent("You have 5 coins");

        JdaPrivateReplier.revealButton().send(event, answer).join();

        Button button = prompt.getValue().getComponents().get(0).asActionRow().getButtons().get(0);
        assertThat(button.getCustomId()).startsWith(JdaRevealReplier.PREFIX);
        assertThat(prompt.getValue().getContent()).doesNotContain("5 coins");

        ButtonInteractionEvent stranger = click(button.getCustomId(), 8L);
        JdaRevealReplier.listener().onEvent(stranger);
        verify(stranger, never()).reply(answer);

        ButtonInteractionEvent owner = click(button.getCustomId(), 7L);
        JdaRevealReplier.listener().onEvent(owner);
        verify(owner).reply(answer);
    }

    @Test
    void anExpiredRevealButtonSaysSo() {
        ButtonInteractionEvent click = click(JdaRevealReplier.PREFIX + "missing", 7L);

        JdaRevealReplier.listener().onEvent(click);

        verify(click).reply("This answer has expired. Run the command again.");
    }

    private static ButtonInteractionEvent click(String id, long userId) {
        ButtonInteractionEvent click = mock(ButtonInteractionEvent.class, RETURNS_DEEP_STUBS);
        when(click.getComponentId()).thenReturn(id);
        when(click.getUser().getIdLong()).thenReturn(userId);
        return click;
    }
}
