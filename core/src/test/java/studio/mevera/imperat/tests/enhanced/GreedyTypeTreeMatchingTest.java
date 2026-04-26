package studio.mevera.imperat.tests.enhanced;

import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.GreedyArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.tests.TestCommandSource;

import java.util.ArrayList;
import java.util.List;

@DisplayName("Greedy Type Tree Matching Tests")
class GreedyTypeTreeMatchingTest extends EnhancedBaseImperatTest {

    @Test
    @DisplayName("Tree probes custom greedy types with the full remaining input")
    void treeUsesFullRemainingInputForCustomGreedyTypes() {
        RecordingMessageType.clearSeenInputs();

        final String value = "Hello my name is Mqzen !";
        var result = execute(
                RecordingBroadcastCommand.class,
                cfg -> cfg.registerArgType(RecordingMessage.class, new RecordingMessageType()),
                "rbm " + value
        );

        assertThat(result)
                .isSuccessful()
                .hasArgumentSatisfying("msg", argument -> Assertions.assertThat(argument)
                                                                  .isInstanceOf(RecordingMessage.class)
                                                                  .extracting(obj -> ((RecordingMessage) obj).message)
                                                                  .isEqualTo(value));

        Assertions.assertThat(RecordingMessageType.seenInputs()).containsOnly(value);
    }

    @RootCommand({"recordingbroadcast", "rbm"})
    public static final class RecordingBroadcastCommand {

        @Execute
        public void exec(TestCommandSource source, RecordingMessage msg) {
            source.reply(msg.message);
        }
    }

    public static final class RecordingMessage {

        private final String message;

        public RecordingMessage(String message) {
            this.message = message;
        }
    }

    public static final class RecordingMessageType extends GreedyArgumentType<TestCommandSource, RecordingMessage> {

        private static final List<String> SEEN_INPUTS = new ArrayList<>();

        static void clearSeenInputs() {
            SEEN_INPUTS.clear();
        }

        static List<String> seenInputs() {
            return List.copyOf(SEEN_INPUTS);
        }

        @Override
        public RecordingMessage parse(
                @NotNull CommandContext<TestCommandSource> context,
                @NotNull Argument<TestCommandSource> argument,
                @NotNull String input
        ) throws CommandException {
            SEEN_INPUTS.add(input);
            return new RecordingMessage(input);
        }
    }
}
