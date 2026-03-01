package studio.mevera.imperat.tests.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.context.ExecutionResult;
import studio.mevera.imperat.tests.BaseImperatTest;
import studio.mevera.imperat.tests.TestSource;

import java.time.Duration;

@DisplayName("Integration Tests")
public class IntegrationTest extends BaseImperatTest {

    @Nested
    @DisplayName("Complex RootCommand Scenarios")
    class ComplexCommandScenarios {

        @Test
        @DisplayName("Should handle complete setrank command")
        void testCompleteSetrankCommand() {
            ExecutionResult<TestSource> result = execute((src) -> src.withPerm("voxy.grant"), "setrank mqzen admin permanent Promotion -e");
            assertSuccess(result);
            assertArgument(result, "target", "mqzen");
            assertArgument(result, "rank", "admin");
            assertArgument(result, "duration", "permanent");
            assertArgument(result, "reason", "Promotion");
            assertFlag(result, "extend", true);
        }

        @Test
        @DisplayName("Should handle ban command with all features")
        void testBanCommandAllFeatures() {
            //Todo: change how flags parsing works in tree.
            // The current implementation requires flags to be placed after all arguments, which is not ideal for user experience.
            ExecutionResult<TestSource> result = execute("ban griefer123 -s -ip 7d Griefing spawn area");
            assertSuccess(result);
            assertArgument(result, "target", "griefer123");
            assertFlag(result, "silent", true);
            assertFlag(result, "ip", true);
            assertArgument(result, "duration", "7d");
            assertArgument(result, "reason", "Griefing spawn area");
        }

        @Test
        @DisplayName("Should handle minimum required ban parameters")
        void testBanCommandMinimalParameters() {
            ExecutionResult<TestSource> result = execute("ban troublemaker");
            assertSuccess(result);
            assertArgument(result, "target", "troublemaker");
            assertFlag(result, "silent", false);
            assertFlag(result, "ip", false);
            assertArgument(result, "duration", "permanent");
            assertArgument(result, "reason", "Breaking server laws"); // Default value
        }
    }

    @Nested
    @DisplayName("Party System Scenarios")
    class PartySystemScenarios {

        @Test
        @DisplayName("Should handle party invite")
        void testPartyInvite() {
            ExecutionResult<TestSource> result = execute("party invite friend123");
            assertSuccess(result);
            assertArgument(result, "receiver", "friend123");
        }

        @Test
        @DisplayName("Should handle party help with pagination")
        void testPartyHelpWithPagination() {
            ExecutionResult<TestSource> result = execute("party help 2");
            assertSuccess(result);
            assertArgument(result, "page", 2);
        }

        @Test
        @DisplayName("Should handle simple party commands")
        void testSimplePartyCommands() {
            ExecutionResult<TestSource> result = execute("party list");
            assertSuccess(result);

            result = execute("party leave");
            assertSuccess(result);

            result = execute("party disband");
            assertSuccess(result);
        }

        @Test
        @DisplayName("Should handle party accept and deny")
        void testPartyAcceptDeny() {
            ExecutionResult<TestSource> result = execute("party accept sender123");
            assertSuccess(result);
            assertArgument(result, "sender", "sender123");

            result = execute("party deny sender456");
            assertSuccess(result);
            assertArgument(result, "sender", "sender456");
        }
    }

    @Nested
    @DisplayName("Rank Management Scenarios")
    class RankManagementScenarios {

        @Test
        @DisplayName("Should handle rank permission assignment")
        void testRankPermissionAssignment() {
            ExecutionResult<TestSource> result = execute("rank addperm moderator worldedit.use");
            assertSuccess(result);
            assertArgument(result, "rank", "moderator");
            assertArgument(result, "permission", "worldedit.use");
            assertFlag(result, "force", false);
        }

        @Test
        @DisplayName("Should handle rank permission with force flag")
        void testRankPermissionWithForce() {
            ExecutionResult<TestSource> result = execute("rank addperm moderator worldedit.use -force");
            assertSuccess(result);
            assertArgument(result, "rank", "moderator");
            assertArgument(result, "permission", "worldedit.use");
            assertFlag(result, "force", true);
        }

        @Test
        @DisplayName("Should handle temporary rank permissions")
        void testTemporaryRankPermissions() {
            ExecutionResult<TestSource> result = execute("rank addperm helper kick.player -customDuration 30d");
            assertSuccess(result);
            assertArgument(result, "rank", "helper");
            assertArgument(result, "permission", "kick.player");
            assertFlag(result, "customDuration", Duration.ofDays(30));
            assertFlag(result, "force", false);
        }

        @Test
        @DisplayName("Should handle rank permissions with all flags")
        void testRankPermissionsAllFlags() {
            ExecutionResult<TestSource> result = execute("rank addperm mod server.fly -customDuration 1d -force");
            assertSuccess(result);
            assertArgument(result, "rank", "mod");
            assertArgument(result, "permission", "server.fly");
            assertFlag(result, "customDuration", Duration.ofDays(1));
            assertFlag(result, "force", true);
        }
    }

    @Nested
    @DisplayName("MOTD RootCommand Scenarios")
    class MOTDCommandScenarios {

        @Test
        @DisplayName("Should handle MOTD with default time")
        void testMOTDDefaultTime() {
            ExecutionResult<TestSource> result = execute("motd Hello world, this is a test message");
            assertSuccess(result);
            assertArgument(result, "message", "Hello world, this is a test message");
        }

        @Test
        @DisplayName("Should handle MOTD with custom time")
        void testMOTDCustomTime() {
            ExecutionResult<TestSource> result = execute("motd -time 1h Hello world, this is a test message");
            assertSuccess(result);
            assertArgument(result, "message", "Hello world, this is a test message");
        }

        @Test
        @DisplayName("Should handle MOTD default execution")
        void testMOTDDefaultExecution() {
            ExecutionResult<TestSource> result = execute("motd");
            assertSuccess(result);
        }
    }

    @Nested
    @DisplayName("Optional Argument Scenarios")
    class OptionalArgumentScenarios {

        @Test
        @DisplayName("Should handle first optional argument with no args")
        void testFirstOptionalArgumentNoArgs() {
            ExecutionResult<TestSource> result = execute("foa");
            assertSuccess(result);
            assertArgument(result, "num", 1); // Default value
        }

        @Test
        @DisplayName("Should handle first optional argument with value")
        void testFirstOptionalArgumentWithValue() {
            ExecutionResult<TestSource> result = execute("foa 5");
            assertSuccess(result);
            assertArgument(result, "num", 5);
        }

        @Test
        @DisplayName("Should handle first optional argument with subcommand")
        void testFirstOptionalArgumentWithSubcommand() {
            ExecutionResult<TestSource> result = execute("foa 3 sub 7");
            assertSuccess(result);
            assertArgument(result, "num", 3);
            assertArgument(result, "num2", 7);
        }

        @Test
        @DisplayName("Should handle first optional argument subcommand default")
        void testFirstOptionalArgumentSubcommandDefault() {
            ExecutionResult<TestSource> result = execute("foa 3 sub");
            assertSuccess(result);
            assertArgument(result, "num", 3);
        }
    }

    @Nested
    @DisplayName("KingdomChat RootCommand Scenarios")
    class KingdomChatScenarios {

        @Test
        @DisplayName("Should handle kingdomchat default")
        void testKingdomChatDefault() {
            ExecutionResult<TestSource> result = execute("kingdomchat");
            assertSuccess(result);
        }

        @Test
        @DisplayName("Should handle greedy argument as the first argument")
        void testKingdomChatWithMessage() {
            ExecutionResult<TestSource> result = execute("kingdomchat hello world everyone");
            assertSuccess(result);
            assertArgument(result, "message", "hello world everyone");
        }
    }

    @Nested
    @DisplayName("Context Resolution Scenarios")
    class ContextResolutionScenarios {

        @Test
        @DisplayName("Should handle context resolved parameters")
        void testContextResolvedParameters() {
            ExecutionResult<TestSource> result = execute("ctx");
            assertSuccess(result);
            // PlayerData should be context-resolved from the test source
        }
    }
}