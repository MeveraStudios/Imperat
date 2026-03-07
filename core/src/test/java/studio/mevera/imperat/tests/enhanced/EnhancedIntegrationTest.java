package studio.mevera.imperat.tests.enhanced;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.context.ExecutionResult;
import studio.mevera.imperat.tests.TestCommandSource;
import studio.mevera.imperat.tests.arguments.TestPlayer;

import java.time.Duration;
import java.util.List;

@DisplayName("Enhanced Integration Tests")
class EnhancedIntegrationTest extends EnhancedBaseImperatTest {

    @Nested
    @DisplayName("Server Administration Scenarios")
    class ServerAdministrationScenarios {

        @Test
        @DisplayName("Should handle complete player punishment workflow")
        void testCompletePlayerPunishmentWorkflow() {
            // Silent ban with IP and custom duration/reason
            ExecutionResult<TestCommandSource> result = execute("ban griefer123 7d -s -ip Destroyed spawn area and harassed players");

            assertThat(result)
                    .isSuccessful()
                    .hasArgument("target", "griefer123")
                    .hasSwitchEnabled("silent")
                    .hasSwitchEnabled("ip")
                    .hasArgument("duration", "7d")
                    .hasArgument("reason", "Destroyed spawn area and harassed players");
        }

        @Test
        @DisplayName("Should handle rank management with temporary permissions")
        void testRankManagementTempPermissions() {
            ExecutionResult<TestCommandSource> result = execute("rank addperm builder worldedit.selection -customDuration 2h -force");

            assertThat(result)
                    .isSuccessful()
                    .hasArgument("rank", "builder")
                    .hasArgument("permission", "worldedit.selection")
                    .hasSwitchEnabled("force")
                    .hasFlagValue("customDuration", Duration.ofHours(2));
        }
    }

    @Nested
    @DisplayName("Player Interaction Scenarios")
    class PlayerInteractionScenarios {

        @Test
        @DisplayName("Should handle item distribution with specific amounts")
        void testItemDistributionSpecificAmounts() {
            ExecutionResult<TestCommandSource> result = execute("give diamond_pickaxe VIP_Player 1");

            assertThat(result)
                    .isSuccessful()
                    .hasArgument("item", "diamond_pickaxe")
                    .hasArgumentOfType("player", TestPlayer.class)
                    .hasArgument("amount", 1);
        }

        @Test
        @DisplayName("Should handle messaging system with long messages")
        void testMessagingSystemLongMessages() {
            ExecutionResult<TestCommandSource> result = execute("message AdminUser Hey there! I'm an engineer");

            assertThat(result)
                    .isSuccessful()
                    .hasArgument("target", "AdminUser")
                    .hasArgument("message", "Hey there! I'm an engineer");
        }
    }

    @Nested
    @DisplayName("Party Management Scenarios")
    class PartyManagementScenarios {

        @Test
        @DisplayName("Should handle complete party creation and management workflow")
        void testCompletePartyWorkflow() {
            // Party invite
            ExecutionResult<TestCommandSource> inviteResult = execute("party invite BestFriend");
            assertThat(inviteResult)
                    .isSuccessful()
                    .hasArgument("receiver", "BestFriend");

            // Party accept
            ExecutionResult<TestCommandSource> acceptResult = execute("party accept PartyLeader");
            assertThat(acceptResult)
                    .isSuccessful()
                    .hasArgument("sender", "PartyLeader");

            // Party list
            ExecutionResult<TestCommandSource> listResult = execute("party list");
            assertThat(listResult).isSuccessful();

            // Party help with pagination
            ExecutionResult<TestCommandSource> helpResult = execute("party help 1");
            assertThat(helpResult)
                    .isSuccessful()
                    .hasArgument("page", 1);
        }

        @Test
        @DisplayName("Should handle party rejection workflow")
        void testPartyRejectionWorkflow() {
            ExecutionResult<TestCommandSource> result = execute("party deny UnwantedInviter");

            assertThat(result)
                    .isSuccessful()
                    .hasArgument("sender", "UnwantedInviter");
        }

        @Test
        @DisplayName("Should handle party dissolution")
        void testPartyDissolution() {
            ExecutionResult<TestCommandSource> leaveResult = execute("party leave");
            assertThat(leaveResult).isSuccessful();

            ExecutionResult<TestCommandSource> disbandResult = execute("party disband");
            assertThat(disbandResult).isSuccessful();
        }
    }

    @Nested
    @DisplayName("Content Management Scenarios")
    class ContentManagementScenarios {

        @Test
        @DisplayName("Should handle MOTD updates with custom durations")
        void testMOTDUpdatesCustomDurations() {
            // Default duration MOTD
            ExecutionResult<TestCommandSource> defaultResult =
                    execute("motd Welcome to our amazing Minecraft server! Have fun and follow the rules.");
            assertThat(defaultResult)
                    .isSuccessful()
                    .hasArgument("message", "Welcome to our amazing Minecraft server! Have fun and follow the rules.");

            // Custom duration MOTD
            ExecutionResult<TestCommandSource> customResult = execute("motd -time 2h Server maintenance scheduled for tonight at 3 AM EST");
            assertThat(customResult)
                    .isSuccessful()
                    .hasArgument("message", "Server maintenance scheduled for tonight at 3 AM EST");
        }

        @Test
        @DisplayName("Should handle kit creation with different weights")
        void testKitCreationDifferentWeights() {
            // Default weight kit
            ExecutionResult<TestCommandSource> defaultKit = execute("kit create starter_kit");
            assertThat(defaultKit)
                    .isSuccessful()
                    .hasArgument("kit", "starter_kit")
                    .hasArgument("weight", 1);

            // Custom weight kit
            ExecutionResult<TestCommandSource> customKit = execute("kit create vip_kit 10");
            assertThat(customKit)
                    .isSuccessful()
                    .hasArgument("kit", "vip_kit")
                    .hasArgument("weight", 10);
        }
    }

    @Nested
    @DisplayName("Advanced RootCommand Combinations")
    class AdvancedCommandCombinations {

        @Test
        @DisplayName("Should handle deeply nested subcommand with inheritance")
        void testDeeplyNestedSubcommandInheritance() {
            ExecutionResult<TestCommandSource> result = execute("test arg1 arg2 sub1 inherited1 sub2 inherited2 sub3 final");

            assertThat(result)
                    .isSuccessful()
                    .hasArgument("otherText", "arg1")
                    .hasArgument("otherText2", "arg2")
                    .hasArgument("a", "inherited1")
                    .hasArgument("b", "inherited2")
                    .hasArgument("c", "final");
        }

        @Test
        @DisplayName("Should handle first optional argument with complex subcommand scenarios")
        void testFirstOptionalArgumentComplexScenarios() {
            // No arguments - should use default
            ExecutionResult<TestCommandSource> defaultResult = execute("foa");
            assertThat(defaultResult)
                    .isSuccessful()
                    .hasArgument("num", 1);

            // With custom value
            ExecutionResult<TestCommandSource> customResult = execute("foa 42");
            assertThat(customResult)
                    .isSuccessful()
                    .hasArgument("num", 42);

            // With subcommand
            ExecutionResult<TestCommandSource> subResult = execute("foa 15 sub 99");
            assertThat(subResult)
                    .isSuccessful()
                    .hasArgument("num", 15)
                    .hasArgument("num2", 99);
        }

        @Test
        @DisplayName("Should return (not last)optional argument suggestions")
        void testEcoCmd() {
            var suggestions = tabComplete("eco add mqzen ");
            Assertions.assertLinesMatch(List.of("gold", "silver"), suggestions);
        }

    }
}