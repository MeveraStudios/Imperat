package studio.mevera.imperat.bukkit.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import studio.mevera.imperat.BaseBrigadierManager;
import studio.mevera.imperat.BukkitCommandSource;
import studio.mevera.imperat.BukkitImperat;
import studio.mevera.imperat.bukkit.test.commands.TestCmd;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.arguments.Argument;

import java.util.List;
import java.util.Objects;

@DisplayName("Bukkit Brigadier Alias Suggestion Tests")
class BukkitBrigadierAliasSuggestionTest {

    private ServerMock server;
    private TestImperatPlugin plugin;
    private BukkitImperat imperat;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(TestImperatPlugin.class);
        imperat = plugin.getImperat();
        imperat.registerCommand(new TestCmd());
        player = server.addPlayer("TestPlayer");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Should expose every first-level subcommand alias through Brigadier suggestions")
    void testBrigadierShowsAllFirstLevelAliases() {
        var suggestions = complete("test ");

        assertEquals(3, suggestions.size());
        assertTrue(suggestions.containsAll(List.of("sub1", "s1", "subone")));
    }

    @Test
    @DisplayName("Should expose nested subcommand aliases when traversing a parent alias through Brigadier")
    void testBrigadierShowsNestedAliasesFromParentAlias() {
        var suggestions = complete("test s1 ");

        assertEquals(3, suggestions.size());
        assertTrue(suggestions.containsAll(List.of("sub2", "s2", "subtwo")));
    }

    private List<String> complete(String input) {
        TestBrigadierManager manager = new TestBrigadierManager(imperat);
        Command<BukkitCommandSource> command = Objects.requireNonNull(imperat.getCommand("test"));
        var dispatcher = new CommandDispatcher<PlayerMock>();
        dispatcher.getRoot().addChild(manager.parseCommandIntoNode(command));
        return dispatcher.getCompletionSuggestions(dispatcher.parse(input, player))
                       .join()
                       .getList()
                       .stream()
                       .map(Suggestion::getText)
                       .toList();
    }

    private static final class TestBrigadierManager extends BaseBrigadierManager<BukkitCommandSource> {

        private final BukkitImperat imperat;

        private TestBrigadierManager(BukkitImperat imperat) {
            super(imperat);
            this.imperat = imperat;
        }

        @Override
        public BukkitCommandSource wrapCommandSource(Object commandSource) {
            return imperat.wrapSender(commandSource);
        }

        @Override
        public ArgumentType<?> getArgumentType(Argument<BukkitCommandSource> parameter) {
            return parameter.isGreedy() ? StringArgumentType.greedyString() : StringArgumentType.word();
        }
    }
}
