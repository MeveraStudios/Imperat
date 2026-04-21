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
import studio.mevera.imperat.bukkit.test.commands.FlagBrigadierCmd;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.arguments.Argument;

import java.util.List;
import java.util.Objects;

@DisplayName("Bukkit Brigadier Flag Suggestion Tests")
class BukkitBrigadierFlagSuggestionTest {

    private ServerMock server;
    private TestImperatPlugin plugin;
    private BukkitImperat imperat;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(TestImperatPlugin.class);
        imperat = plugin.getImperat();
        imperat.registerCommand(new FlagBrigadierCmd());
        player = server.addPlayer("TestPlayer");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Should merge root literals with flag suggestions through Brigadier")
    void testBrigadierShowsRootFlagsAndSubcommands() {
        var suggestions = complete("flagtest ");

        assertEquals(4, suggestions.size());
        assertTrue(suggestions.containsAll(List.of("play", "mix", "-scenario", "-sc")));
    }

    @Test
    @DisplayName("Should suggest root flag values through Brigadier")
    void testBrigadierShowsRootFlagValues() {
        var suggestions = complete("flagtest -sc ");

        assertEquals(4, suggestions.size());
        assertTrue(suggestions.containsAll(List.of("kindergarten", "castle", "sandstorm", "tsunami")));
    }

    @Test
    @DisplayName("Should suggest subcommand flag names through Brigadier")
    void testBrigadierShowsSubcommandFlagNames() {
        var suggestions = complete("flagtest play ");

        assertEquals(2, suggestions.size());
        assertTrue(suggestions.containsAll(List.of("-scenario", "-sc")));
    }

    @Test
    @DisplayName("Should suggest subcommand flag values through Brigadier")
    void testBrigadierShowsSubcommandFlagValues() {
        var suggestions = complete("flagtest play -sc ");

        assertEquals(4, suggestions.size());
        assertTrue(suggestions.containsAll(List.of("kindergarten", "castle", "sandstorm", "tsunami")));
    }

    @Test
    @DisplayName("Should apply flag value suggestions without replacing the flag input")
    void testBrigadierAppliesFlagValueSuggestionAtValueOffset() {
        String input = "flagtest play --scenario ";
        Suggestion suggestion = completeSuggestions(input)
                                        .stream()
                                        .filter(candidate -> candidate.getText().equals("kindergarten"))
                                        .findFirst()
                                        .orElseThrow();

        assertEquals("flagtest play --scenario kindergarten", suggestion.apply(input));
    }

    @Test
    @DisplayName("Should suggest post-argument flag names through Brigadier")
    void testBrigadierShowsFlagsAfterArguments() {
        var suggestions = complete("flagtest mix player ");

        assertEquals(2, suggestions.size());
        assertTrue(suggestions.containsAll(List.of("-scenario", "-sc")));
    }

    @Test
    @DisplayName("Should suggest post-argument flag values through Brigadier")
    void testBrigadierShowsFlagValuesAfterArguments() {
        var suggestions = complete("flagtest mix player -sc ");

        assertEquals(4, suggestions.size());
        assertTrue(suggestions.containsAll(List.of("kindergarten", "castle", "sandstorm", "tsunami")));
    }

    private List<String> complete(String input) {
        return completeSuggestions(input)
                       .stream()
                       .map(Suggestion::getText)
                       .toList();
    }

    private List<Suggestion> completeSuggestions(String input) {
        TestBrigadierManager manager = new TestBrigadierManager(imperat);
        Command<BukkitCommandSource> command = Objects.requireNonNull(imperat.getCommand("flagtest"));
        var dispatcher = new CommandDispatcher<PlayerMock>();
        dispatcher.getRoot().addChild(manager.parseCommandIntoNode(command));
        return dispatcher.getCompletionSuggestions(dispatcher.parse(input, player))
                       .join()
                       .getList();
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
