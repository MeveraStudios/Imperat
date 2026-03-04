package studio.mevera.imperat.tests.enhanced;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.tests.commands.realworld.AnnounceCommand;
import studio.mevera.imperat.tests.commands.realworld.ShoutCommand;

/**
 * Tests that switches and greedy string arguments interact correctly
 * regardless of whether the switches are declared before or after the greedy param.
 *
 * <ul>
 *   <li>{@code /announce [-urgent/-u] [-pin/-p] <message...>} — switches BEFORE greedy</li>
 *   <li>{@code /shout <message...> [-loud/-l] [-bold/-b]}     — switches AFTER greedy</li>
 * </ul>
 */
@DisplayName("Switch + Greedy string interaction tests")
class SwitchWithGreedyTest extends EnhancedBaseImperatTest {

    // ════════════════════════════════════════════════════════════════════════
    // Switches declared BEFORE the greedy parameter (/announce)
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Switches declared BEFORE greedy — /announce [-urgent/-u] [-pin/-p] <message...>")
    class SwitchesBeforeGreedy {

        @Test
        @DisplayName("Greedy only — no switches: 'announce Hello world' → message='Hello world'")
        void greedyOnlyNoSwitches() {
            assertThat(execute(AnnounceCommand.class, cfg -> {
            }, "announce Hello world"))
                    .isSuccessful()
                    .hasSwitchDisabled("urgent")
                    .hasSwitchDisabled("pin")
                    .hasArgument("message", "Hello world");
        }

        @Test
        @DisplayName("Single switch before greedy: 'announce -u Hello world' → urgent=true, message='Hello world'")
        void singleSwitchBeforeGreedy() {
            assertThat(execute(AnnounceCommand.class, cfg -> {
            }, "announce -u Hello world"))
                    .isSuccessful()
                    .hasSwitchEnabled("urgent")
                    .hasSwitchDisabled("pin")
                    .hasArgument("message", "Hello world");
        }

        @Test
        @DisplayName("Two switches before greedy: 'announce -u -p Important update' → both enabled")
        void twoSwitchesBeforeGreedy() {
            assertThat(execute(AnnounceCommand.class, cfg -> {
            }, "announce -u -p Important update"))
                    .isSuccessful()
                    .hasSwitchEnabled("urgent")
                    .hasSwitchEnabled("pin")
                    .hasArgument("message", "Important update");
        }

        @Test
        @DisplayName("Switch using full name: 'announce -urgent Server restarting soon'")
        void switchFullNameBeforeGreedy() {
            assertThat(execute(AnnounceCommand.class, cfg -> {
            }, "announce -urgent Server restarting soon"))
                    .isSuccessful()
                    .hasSwitchEnabled("urgent")
                    .hasSwitchDisabled("pin")
                    .hasArgument("message", "Server restarting soon");
        }

        @Test
        @DisplayName("Switch after greedy content: 'announce Hello world -u'")
        void switchAfterGreedyContent() {
            assertThat(execute(AnnounceCommand.class, cfg -> {
            }, "announce Hello world -u"))
                    .isSuccessful()
                    .hasSwitchEnabled("urgent")
                    .hasSwitchDisabled("pin")
                    .hasArgument("message", "Hello world");
        }

        @Test
        @DisplayName("Switches surrounding greedy: 'announce -u Hello world -p'")
        void switchesSurroundingGreedy() {
            assertThat(execute(AnnounceCommand.class, cfg -> {
            }, "announce -u Hello world -p"))
                    .isSuccessful()
                    .hasSwitchEnabled("urgent")
                    .hasSwitchEnabled("pin")
                    .hasArgument("message", "Hello world");
        }

        @Test
        @DisplayName("Long greedy with switch before")
        void longGreedyWithSwitchBefore() {
            assertThat(execute(AnnounceCommand.class, cfg -> {
                    },
                    "announce -p This is a very long announcement with many words"))
                    .isSuccessful()
                    .hasSwitchDisabled("urgent")
                    .hasSwitchEnabled("pin")
                    .hasArgument("message", "This is a very long announcement with many words");
        }

        @Test
        @DisplayName("Single word greedy with switch: 'announce -u Hello'")
        void singleWordGreedyWithSwitch() {
            assertThat(execute(AnnounceCommand.class, cfg -> {
            }, "announce -u Hello"))
                    .isSuccessful()
                    .hasSwitchEnabled("urgent")
                    .hasArgument("message", "Hello");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Switches declared AFTER the greedy parameter (/shout)
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Switches declared AFTER greedy — /shout <message...> [-loud/-l] [-bold/-b]")
    class SwitchesAfterGreedy {

        @Test
        @DisplayName("Greedy only — no switches: 'shout Hello world' → message='Hello world'")
        void greedyOnlyNoSwitches() {
            assertThat(execute(ShoutCommand.class, cfg -> {
            }, "shout Hello world"))
                    .isSuccessful()
                    .hasSwitchDisabled("loud")
                    .hasSwitchDisabled("bold")
                    .hasArgument("message", "Hello world");
        }

        @Test
        @DisplayName("Switch at end: 'shout Hello world -l' → loud=true, message='Hello world'")
        void switchAtEnd() {
            assertThat(execute(ShoutCommand.class, cfg -> {
            }, "shout Hello world -l"))
                    .isSuccessful()
                    .hasSwitchEnabled("loud")
                    .hasSwitchDisabled("bold")
                    .hasArgument("message", "Hello world");
        }

        @Test
        @DisplayName("Two switches at end: 'shout Hello world -l -b' → both enabled, message='Hello world'")
        void twoSwitchesAtEnd() {
            assertThat(execute(ShoutCommand.class, cfg -> {
            }, "shout Hello world -l -b"))
                    .isSuccessful()
                    .hasSwitchEnabled("loud")
                    .hasSwitchEnabled("bold")
                    .hasArgument("message", "Hello world");
        }

        @Test
        @DisplayName("Switch in middle of greedy content: 'shout Hello -l world' → loud=true, message='Hello world'")
        void switchInMiddleOfGreedy() {
            assertThat(execute(ShoutCommand.class, cfg -> {
            }, "shout Hello -l world"))
                    .isSuccessful()
                    .hasSwitchEnabled("loud")
                    .hasArgument("message", "Hello");
        }

        @Test
        @DisplayName("Switch before greedy content: 'shout -b Important message here' → bold=true, message='Important message here'")
        void switchBeforeGreedyContent() {
            assertThat(execute(ShoutCommand.class, cfg -> {
            }, "shout -b Important message here"))
                    .isSuccessful()
                    .hasSwitchDisabled("loud")
                    .hasSwitchEnabled("bold")
                    .hasArgument("message", "Important message here");
        }

        @Test
        @DisplayName("Switches surrounding greedy: 'shout -l Hello world -b' → both enabled, message='Hello world'")
        void switchesSurroundingGreedy() {
            assertThat(execute(ShoutCommand.class, cfg -> {
            }, "shout -l Hello world -b"))
                    .isSuccessful()
                    .hasSwitchEnabled("loud")
                    .hasSwitchEnabled("bold")
                    .hasArgument("message", "Hello world");
        }

        @Test
        @DisplayName("Full name switches: 'shout -loud -bold Warning everyone' → both enabled")
        void fullNameSwitches() {
            assertThat(execute(ShoutCommand.class, cfg -> {
            }, "shout -loud -bold Warning everyone"))
                    .isSuccessful()
                    .hasSwitchEnabled("loud")
                    .hasSwitchEnabled("bold")
                    .hasArgument("message", "Warning everyone");
        }

        @Test
        @DisplayName("Long greedy with switch at end: 'shout This is a very long message -l'")
        void longGreedyWithSwitchAtEnd() {
            assertThat(execute(ShoutCommand.class, cfg -> {
                    },
                    "shout This is a very long message with many words -l"))
                    .isSuccessful()
                    .hasSwitchEnabled("loud")
                    .hasSwitchDisabled("bold")
                    .hasArgument("message", "This is a very long message with many words");
        }

        @Test
        @DisplayName("Single word greedy with switch: 'shout Hello -b' → message='Hello'")
        void singleWordGreedyWithSwitch() {
            assertThat(execute(ShoutCommand.class, cfg -> {
            }, "shout Hello -b"))
                    .isSuccessful()
                    .hasSwitchEnabled("bold")
                    .hasArgument("message", "Hello");
        }
    }
}
