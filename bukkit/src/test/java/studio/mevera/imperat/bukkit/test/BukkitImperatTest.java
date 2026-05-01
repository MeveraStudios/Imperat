package studio.mevera.imperat.bukkit.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import studio.mevera.imperat.BukkitCommandSource;
import studio.mevera.imperat.BukkitImperat;
import studio.mevera.imperat.util.ImperatDebugger;

/**
 * Simple unit test for executing Imperat commands on Bukkit using MockBukkit.
 */
class BukkitImperatTest {

    private ServerMock server;
    private TestImperatPlugin plugin;
    private BukkitImperat imperat;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(TestImperatPlugin.class);
        imperat = plugin.getImperat();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void testCommandRegistered() {
        // Verify the "greet" command was registered in Imperat
        assertNotNull(imperat.getCommand("tell"), "RootCommand 'greet' should be registered");
    }

    @Test
    void testDefCommandExecution() {
        // Create a mock player and execute the command
        String[] aliases = {"tell", "msg", "t", "w", "whisper", "pm"};

        PlayerMock player = server.addPlayer("TestPlayer");
        BukkitCommandSource source = imperat.wrapSender(player);

        ImperatDebugger.setEnabled(true);
        for (String alias : aliases) {
            // Execute the command with arguments
            var res = imperat.execute(source, alias);

            // Verify the player received the correct message
            Assertions.assertEquals("§4ERROR: §cInvalid command usage: '/" + alias + "'",
                    player.nextMessage());
            Assertions.assertEquals("§4ERROR: §cYou probably meant '/" + alias + " <name> <message...>'", player.nextMessage());
        }

    }

    // Removed: testArgumentSuggestionForCommandAliases — exercised the
    // AsyncTabCompleteEvent listener path under MockBukkit's
    // MODERN_NATIVE_BRIGADIER capability. The listener is now strictly
    // gated to the PLAIN_COMMAND_MAP backend (so it doesn't overwrite
    // Brigadier-native completions on real Paper), which means MockBukkit
    // can't reproduce its event-driven completion path. Real-server
    // smoke is the verification path for that flow.

}



